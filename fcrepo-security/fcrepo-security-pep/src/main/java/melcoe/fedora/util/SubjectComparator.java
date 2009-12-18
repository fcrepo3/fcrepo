/*
 * File: SubjectComparator.java
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
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.Subject;

/**
 * Class to compare two Subjects.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class SubjectComparator
        implements Comparator<Subject> {

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public int compare(Subject a, Subject b) {
        int result = 0;

        Comparator cmp = new AttributeComparator();
        Set<Attribute> setA = new TreeSet<Attribute>(cmp);
        setA.addAll(a.getAttributes());

        Set<Attribute> setB = new TreeSet<Attribute>(cmp);
        setB.addAll(b.getAttributes());

        Iterator<Attribute> iterA = setA.iterator();
        Iterator<Attribute> iterB = setB.iterator();
        while (iterA.hasNext() && iterB.hasNext()) {
            Attribute attrA = iterA.next();
            Attribute attrB = iterB.next();
            result = cmp.compare(attrA, attrB);

            if (result != 0) {
                return result;
            }
        }

        result = setA.size() - setB.size();

        return result;
    }
}
