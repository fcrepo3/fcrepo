/*
 * File: AttributeComparator.java
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

package melcoe.fedora.util;

import java.util.Comparator;

import com.sun.xacml.ctx.Attribute;

/**
 * Class to compare two Attributes.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class AttributeComparator
        implements Comparator {

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object o1, Object o2) {
        Attribute a = (Attribute) o1;
        Attribute b = (Attribute) o2;

        int result = a.getId().toString().compareTo(b.getId().toString());

        if (result == 0) {
            result = a.getType().toString().compareTo(b.getType().toString());
        } else {
            return result;
        }

        if (result == 0) {
            result = a.getValue().encode().compareTo(b.getValue().encode());
        } else {
            return result;
        }

        if (result == 0
                && (a.getIssueInstant() != null || b.getIssueInstant() != null)) {
            if (a.getIssueInstant() == null && b.getIssueInstant() == null) {
                result = 0;
            } else if (a.getIssueInstant() != null
                    && b.getIssueInstant() == null) {
                result = 1;
            } else if (a.getIssueInstant() == null
                    && b.getIssueInstant() != null) {
                result = -1;
            } else {
                result =
                        a.getIssueInstant().encode().compareTo(b
                                .getIssueInstant().encode());
            }
        }

        return result;
    }
}
