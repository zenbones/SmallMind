# Native Image Review: `nutsnbolts` reflection

Working notes for picking up GraalVM native-image readiness of
`nutsnbolts/src/main/java/org/smallmind/nutsnbolts/reflection` (packages `reflection`, `reflection.aop`,
`reflection.bean`, `reflection.type`). Review only; no code has been changed yet.

- Reviewed against commit `85f431e0c` ("modular reflections").
- Line numbers below are file line numbers at that commit; re-check them before editing.
- Paths are relative to `nutsnbolts/src/main/java/org/smallmind/nutsnbolts/` unless stated otherwise.

## Current state

- No native-image metadata ships anywhere in the repository (no `META-INF/native-image/...` in any module).
- No root-level `.adoc` chapter mentions native image except `KAFKA.adoc`.
- `NUTSNBOLTS.adoc` has no native-image guidance.

## Background: how native image changes reflection behavior

- A closed-world native image cannot define new classes at runtime.
- `.class` files are not present in the image, so reading class bytes as resources fails.
- In the default mode, **bulk** reflective queries (`getDeclaredFields`, `getMethods`, `getDeclaredMethods`)
  return only the members that were registered. Unregistered members are omitted, with no error.
- In the default mode, **single** lookups (`getMethod`, `getDeclaredMethod`) throw `NoSuchMethodException` for
  members that exist but are not registered.
- In exact reachability metadata mode (`--exact-reachability-metadata` on recent GraalVM releases), both cases
  throw `MissingReflectionRegistrationError` instead. That mode is much safer for this code, because several
  call sites below treat "not found" as a normal outcome.
- `Method.invoke` needs the method registered for invocation, not just for query.
- `MethodHandles`, `privateLookupIn`, and `ClassValue` are supported, given registered members.

## Findings by class

### Will not work in a native image

#### `reflection/ProxyGenerator.java`

Fundamentally incompatible: it generates byte code with ASM and defines the class at runtime.

- Runtime class definition at `:720` (`proxiedLookup.defineClass`) and `:850` (`ProxyClassLoader.defineClass`).
- It fails earlier than that in practice. `assembleHierarchy` always appends `ObjectImpersonator`, and every
  non-JDK type in the hierarchy is read through `ByteCodeReader`, which needs the `.class` resource. So even a
  proxy of a JDK interface fails, with the message "its class loader does not provide it as a resource". That
  message does not point at native image as the cause.
- If someone includes the `.class` files as resources to get past that, `defineClass` fails with a GraalVM
  unsupported-feature error. To verify: I believe that error is not a `LinkageError`, in which case it escapes
  the `catch (IllegalAccessException | LinkageError ...)` at `:722` and the
  `catch (ClassNotFoundException | LinkageError ...)` at `:746` without being wrapped in
  `ByteCodeManipulationException`.
- GraalVM's "predefined classes" support (bytes recorded by the tracing agent) only matches classes whose bytes
  are identical at runtime. The proxy name includes `PROXY_CLASS_COUNTER` (`:209`), so it matches only if
  proxies are always created in the same order. Not a viable route.
- `visitReflectively` (`:317`, `:320`) uses `getDeclaredConstructors` / `getDeclaredMethods` on JDK types; moot
  given the above.
- The generated `<clinit>` resolves methods with `Class.forName` + `getMethod`; also moot.

**Proposed change:** at the top of `createProxy`, detect native image and fail with a clear message:

```java
if ("runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"))) {
  throw new ByteCodeManipulationException(...);
}
```

This needs no GraalVM SDK dependency. The guidance for native-image callers is to use
`java.lang.reflect.Proxy` for interfaces, with dynamic-proxy registration.

#### `reflection/ByteCodeReader.java` and `reflection/ClassInspector.java`

- `ByteCodeReader.createClassReader` reads `clazz.getModule().getResourceAsStream(internalName + ".class")`
  at `:64`. It returns `null` in an image, so it throws `ByteCodeManipulationException`.
- `ClassInspector.trace` and `ClassInspector.asmify` go through `ByteCodeReader`, so they fail the same way.
  `ClassInspector` is a development tool, so this probably does not matter beyond a clear message.
- The same early native-image check could go in `ByteCodeReader.createClassReader`, which would cover
  `ClassInspector` and `ProxyGenerator` in one place. Decide whether one check there is enough or whether
  `ProxyGenerator` also wants its own.

### Works, if the caller registers metadata

The main risk here is behavior that changes **silently** when metadata is missing.

#### `reflection/FieldUtility.java`, `reflection/FieldAccessor.java`, `reflection/Overlay.java`

- `FieldUtility.assembleFieldAccessors` calls `getDeclaredFields()` at `:195`. Unregistered fields are
  omitted with no error, so `Overlay` silently skips them.
- `FieldUtility.locateGetter` / `locateSetter` call `getMethod` at `:332`, `:334`, `:357` and catch
  `NoSuchMethodException` as "no getter/setter". An unregistered getter or setter is therefore bypassed, and
  the field is read or written directly, skipping the getter's or setter's logic. No error.
- `FieldUtility.ensureReadable` (`:236`) does `lookup.findVirtual(Module.class, "addReads", ...)` on a lookup
  that is not a build-time constant. Classpath builds never reach it (the unnamed module reads every module).
  Module-path builds may need `java.lang.Module.addReads(Module)` registered. To verify.
- The `MethodHandle` design itself is native-friendly. `unreflectGetter` / `unreflectSetter` need the field
  registered. A `final` field is never written directly (the setter handle is `null`), so `allowWrite` is never
  needed.
- `ClassValue` caches and `ClassLoaderAncestry` work as-is. In a classpath image every application class has
  the same loader, so the caches take the non-ancestor branch.
- `Overlay.equivalentToNull` (`:143`) reads `field.getAnnotations()` and the `@OverlayNullifier`
  meta-annotation on the annotation type. Field annotations come with the field's registration. The annotation
  type may also need registering. To verify.
- `Overlay.internalEquivalentToNull` (`:163`) calls `lookup.findConstructor(validatedBy, ()V)`. The
  validator's no-arg constructor must be registered.

**Proposed change:** nutsnbolts registers the no-arg constructors of its own validators in a shipped metadata
file, since the module owns them:

- `EmptyStringNullifierValidator`
- `NegativeIntegerNullifierValidator`
- `NonPositiveIntegerNullifierValidator`
- `StartOfEpochNullifierValidator`

#### `reflection/bean/BeanUtility.java` (and `reflection/aop/AOPUtility.java` through it)

- `acquireCandidateMethods` calls `getMethods()` at `:270`. Unregistered methods are omitted with no error.
- `getMethod` at `:423` and `findAccessibleMethod` at `:477` throw `NoSuchMethodException` for unregistered
  methods. That surfaces as `BeanAccessException` "No 'getter' method(...) found in class(...)", which is
  misleading when the method exists but is not registered.
- `findAccessibleMethod` walks interfaces and superclasses. For a non-public JDK implementation class (for
  example `Collections$UnmodifiableList`) the interface methods also have to be registered.
- `getParameterClass` reads `@TypeHint` through `getParameterAnnotations()` (`:53`), so the setter must be
  registered.
- `AOPUtility` works with AspectJ compile-time weaving; ajc stores parameter names as strings, not through
  reflection. Load-time weaving cannot work in a native image.

#### `reflection/Getter.java`, `reflection/Setter.java`, `reflection/OffloadingInvocationHandler.java`

- `Method.invoke` in `Getter.invoke` (`Getter.java:97`), `Setter.invoke`, and
  `OffloadingInvocationHandler.invoke` (`OffloadingInvocationHandler.java:54`) needs the method registered for
  invocation, not just query.
- `Getter` and `Setter` are `Serializable`: serialization registration is needed, and the
  `getDeclaredMethod` calls in `readObject` (`Getter.java:171`, `Setter.java:156`) need reflection
  registration.
- `OffloadingInvocationHandler` is normally used behind a `java.lang.reflect.Proxy`, which needs its own
  dynamic-proxy registration.

#### `reflection/type/GenericUtility.java`

- `getGenericSuperclass`, `getGenericInterfaces`, `getTypeParameters` (`:56`, `:92`, `:143`) need the classes
  registered. My understanding (to verify) is that without registration they return raw types, so
  `getTypeArgumentsOfSubclass` ends with `UnexpectedGenericDeclaration` rather than the type argument, with a
  message that does not point at missing metadata.
- `getClass` calls `Array.newInstance(componentClass, 0)` at `:232`, which needs the array type reachable or
  registered.

### No native-image concerns

- `AnnotationFilter`, `PassType`, `Operation`, `type/TypeInference`, `bean/TypeHint`
- all exception types (`ByteCodeManipulationException`, `ReflectionContractException`, `OverlayException`,
  `MissingAnnotationException`, `bean/BeanAccessException`, `bean/BeanInvocationException`,
  `type/TypeInferenceException`, `type/UnexpectedGenericDeclaration`)
- `ClassLoaderAncestry`
- the `*Nullifier` annotations, `OverlayNullifier`, `OverlayNullifierValidator`, `Differentiable`
- `ObjectImpersonator` (only relevant as an input to `ProxyGenerator`)
- `ParameterIterable` (entirely commented out)
- `type/TypeUtility`: the reflective `valueOf` call in `box` (`:91`) is never reached, because
  `value.getClass()` is never primitive. That is a separate, non-native bug: the branch is dead code.

## Next steps

1. **Early failure in byte-code paths.** Add the `org.graalvm.nativeimage.imagecode` check to
   `ProxyGenerator.createProxy` and/or `ByteCodeReader.createClassReader`, throwing
   `ByteCodeManipulationException` with a message that names native image. Add a test that sets the system
   property and asserts the message (restore the property afterwards).
2. **Ship module-owned metadata.** Add
   `nutsnbolts/src/main/resources/META-INF/native-image/org.smallmind/nutsnbolts/reachability-metadata.json`
   registering the four nullifier validators' no-arg constructors. Decide whether to also register
   `java.lang.Module.addReads(Module)` for module-path builds.
3. **Document it in `NUTSNBOLTS.adoc`.** Read `ASCIIDOC_DOC_AUTHORING_GUIDE.md` first. Keep it user-facing
   (what users configure and observe; no internals or review narration). Cover:
   - which classes work and which do not (`ProxyGenerator`, `ClassInspector`);
   - what callers must register: fields, getters/setters, invoked methods, generic signatures, serialization,
     dynamic proxies, their own nullifier validators;
   - the silent-degradation behaviors: skipped fields, bypassed getters/setters, misleading
     "No 'getter' method" / `UnexpectedGenericDeclaration` failures;
   - recommended setup: run tests or the application under the tracing agent
     (`-agentlib:native-image-agent`) and build with exact reachability metadata mode.
4. **Verify the hedged points** before documenting them:
   - whether GraalVM's runtime class-definition error is a `LinkageError` (affects the catch blocks in
     `ProxyGenerator`);
   - whether `ensureReadable`'s `findVirtual(Module.class, "addReads", ...)` needs registration on module-path
     builds;
   - what `getGenericSuperclass` and friends return for unregistered classes;
   - whether annotation types need registering for `annotationType().getAnnotation(OverlayNullifier.class)`;
   - the current name and availability of the exact reachability metadata flag for the targeted GraalVM release.
5. **Optional, separate fix:** the dead reflective branch in `TypeUtility.box`.
