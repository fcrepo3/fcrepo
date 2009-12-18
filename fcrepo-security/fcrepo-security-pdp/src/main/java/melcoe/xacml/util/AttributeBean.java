/*
 * File: AttributeBean.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package melcoe.xacml.util;

import java.util.HashSet;
import java.util.Set;

/**
 * This utility class provides a container for the basic information of an
 * Attribute.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class AttributeBean {

    private String id;

    private String type;

    private Set<String> values;

    /**
     * Default constructor.
     */
    public AttributeBean() {
        // Default constructor
        values = new HashSet<String>();
    }

    /**
     * The parameterised constructor that creates a n object with values
     * initialised.
     * 
     * @param id
     *        the id of the Attribute
     * @param type
     *        the type of the Attribute
     * @param values
     *        the value of the Attribute
     */
    public AttributeBean(String id, String type, Set<String> values) {
        this.id = id;
        this.type = type;
        this.values = values;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *        the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *        the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the value
     */
    public Set<String> getValues() {
        return values;
    }

    /**
     * @param values
     *        the value to set
     */
    public void setValues(Set<String> values) {
        this.values = values;
    }

    /**
     * Adds a value for this attribute.
     * 
     * @param value
     */
    public void addValue(String value) {
        values.add(value);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (id == null ? 0 : id.hashCode());
        result = prime * result + (type == null ? 0 : type.hashCode());
        result = prime * result + (values == null ? 0 : values.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AttributeBean other = (AttributeBean) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (values == null) {
            if (other.values != null) {
                return false;
            }
        } else if (!values.equals(other.values)) {
            return false;
        }
        return true;
    }
}
