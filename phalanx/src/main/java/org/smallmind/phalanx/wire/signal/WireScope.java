/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire.signal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import org.smallmind.nutsnbolts.scope.Scope;

/**
 * Abstract base for scope objects carried alongside a wire invocation; subclasses hold domain-specific data (a
 * security token, a trace identifier) that must cross the service boundary with the call. This is the
 * {@link Scope} counterpart of {@link WireContext}.
 * <p>
 * On the wire a scope is a single-key JSON object whose key is the scope's tag, the {@code @XmlRootElement} name
 * when the class declares one and the simple class name otherwise, and whose value is the scope serialized through
 * {@code JsonCodec} with {@link XmlAccessType#PROPERTY} access. The receiving side resolves the tag through
 * {@link org.smallmind.phalanx.wire.WireScopeManager}; a tag with no registered class is preserved as a
 * {@link ProtoWireScope} rather than dropped.
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public abstract class WireScope implements Scope {

}
