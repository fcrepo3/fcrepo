package org.fcrepo.utilities.install;

        
public enum LLStoreType {
    akubra_fs {
        @Override
        public String toString() {
            return "akubra-fs";
        }
    },
    legacy_fs {
        @Override
        public String toString() {
            return "legacy-fs";
        }
    }
}
    