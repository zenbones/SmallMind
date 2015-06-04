package org.smallmind.phalanx.wire;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.jersey.fault.Fault;
import org.smallmind.web.jersey.fault.FaultElement;

public class WireInvocationCircuit {

  private final ConcurrentHashMap<ServiceKey, MethodInvoker> invokerMap = new ConcurrentHashMap<>();

  public void register (Class<?> serviceInterface, WiredService targetService)
    throws NoSuchMethodException, ServiceDefinitionException {

    invokerMap.putIfAbsent(new ServiceKey(targetService.getVersion(), targetService.getServiceName()), new MethodInvoker(targetService, serviceInterface));
  }

  public void handle (ResponseTransport transport, SignalCodec signalCodec, String callerId, String messageId, InvocationSignal invocationSignal) {

    MethodInvoker methodInvoker;
    Object result = null;
    String nativeType = null;
    boolean error = false;

    try {
      try {

        Function invocationFunction;
        Methodology methodology;
        Object[] arguments;

        if ((methodInvoker = invokerMap.get(new ServiceKey(invocationSignal.getAddress().getLocation().getVersion(), invocationSignal.getAddress().getLocation().getService()))) == null) {
          throw new ServiceDefinitionException("Unregistered service(version = %d, name = %s)", invocationSignal.getAddress().getLocation().getVersion(), invocationSignal.getAddress().getLocation().getService());
        }
        if ((invocationFunction = invocationSignal.getAddress().getLocation().getFunction()).isPartial()) {

          Function completeFunction;

          if ((completeFunction = methodInvoker.match(invocationFunction)) == null) {
            throw new MissingInvocationException("Unable to locate the proper method for the partial function(%s) of service(%s)", invocationFunction.getName(), invocationSignal.getAddress().getLocation().getService());
          }

          invocationFunction = completeFunction;
        }

        methodology = methodInvoker.getMethodology(invocationFunction);
        arguments = new Object[invocationFunction.getSignature().length];
        if (invocationSignal.getArguments() != null) {
          for (Map.Entry<String, Object> argumentEntry : invocationSignal.getArguments().entrySet()) {

            ArgumentInfo argumentInfo;

            if ((argumentInfo = methodology.getArgumentInfo(argumentEntry.getKey())) == null) {
              throw new MismatchedArgumentException("Invocation argument(%s) on method(%s) of service(%s) can't be matched by name", argumentEntry.getKey(), invocationFunction.getName(), invocationSignal.getAddress().getLocation().getService());
            }
            if (argumentInfo.getIndex() >= arguments.length) {
              throw new MismatchedArgumentException("Invocation argument(%s) on method(%s) of service(%s) maps to a non-existent argument index(%d)", argumentEntry.getKey(), invocationFunction.getName(), invocationSignal.getAddress().getLocation().getService(), argumentInfo.getIndex());
            }

            arguments[argumentInfo.getIndex()] = signalCodec.extractObject(argumentEntry.getValue(), argumentInfo.getParameterType());
          }
        }

        nativeType = invocationFunction.getNativeType();
        result = methodInvoker.remoteInvocation(invocationSignal.getContexts(), invocationFunction, arguments);

        if ((result != null) && (!(result instanceof Serializable))) {
          throw new TransportException("The result(%s) of this call is not Serializable", result.getClass().getName());
        }
      } catch (Exception exception) {
        if (!invocationSignal.isInOnly()) {
          error = true;
          result = new Fault(new FaultElement(invocationSignal.getAddress().getLocation().getService(), invocationSignal.getAddress().getLocation().getFunction().getName()), exception);
        }
      }

      if (!invocationSignal.isInOnly()) {
        transport.transmit(callerId, messageId, error, nativeType, result);
      }
    } catch (Throwable throwable) {
      LoggerManager.getLogger(WireInvocationCircuit.class).error(throwable);
    }
  }

  private static class ServiceKey {

    private String service;
    private int version;

    private ServiceKey (int version, String service) {

      this.version = version;
      this.service = service;
    }

    public String getService () {

      return service;
    }

    public int getVersion () {

      return version;
    }

    @Override
    public int hashCode () {

      return service.hashCode() ^ version;
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof ServiceKey) && ((ServiceKey)obj).getService().equals(service) && (((ServiceKey)obj).getVersion() == version);
    }
  }
}
