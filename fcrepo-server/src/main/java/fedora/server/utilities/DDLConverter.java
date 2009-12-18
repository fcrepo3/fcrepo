/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.util.List;

/**
 * Interface for a converter of TableSpec objects to RDBMS-specific DDL code.
 * <p>
 * Implementations of this class must be thread-safe. That is, one instance can
 * be used simultanously without problems. This typically just means that no
 * varying fields should be used.
 * </p>
 * <p>
 * Implementations must also have a public no-arg constructor.
 * </p>
 *
 * @author Chris Wilper
 */
public interface DDLConverter {

    public abstract List<String> getDDL(TableSpec tableSpec);

}
