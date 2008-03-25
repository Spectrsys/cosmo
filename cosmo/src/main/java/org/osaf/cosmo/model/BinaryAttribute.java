/*
 * Copyright 2007-2008 Open Source Applications Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osaf.cosmo.model;

import java.io.InputStream;

/**
 * Attribute that stores a binary value.
 */
public interface BinaryAttribute extends Attribute{

    // Property accessors
    public byte[] getValue();

    public void setValue(byte[] value);

    /**
     * @return legnth of data
     */
    public int getLength();

    /**
     * @return inputstream to data
     */
    public InputStream getInputStream();

}
