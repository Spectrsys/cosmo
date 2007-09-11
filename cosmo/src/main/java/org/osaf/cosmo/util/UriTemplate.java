/*
 * Copyright 2007 Open Source Applications Foundation
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
package org.osaf.cosmo.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * Models a URI pattern such that candidate URIs can be matched
 * against the template to extract interesting information from them.
 * </p>
 * <p>
 * A URI pattern looks like
 * <code>/collection/{uid}/{projection}?/{format}?/*</code>. Each path
 * segment can be either a literal or a variable (the latter enclosed
 * in curly braces}. A segment can be further denoted as optional, in
 * which case the segment is trailed by a question mark. A final segment of
 * <code>*</code> indicates that the remainder of the candidate URI after the
 * previous segment is matched.
 * </p>
 * <p>
 * Inspired by the .NET UriTemplate class.
 * </p>
 */
public class UriTemplate {
    private static final Log log = LogFactory.getLog(UriTemplate.class);

    private String pattern;
    private String base;
    private ArrayList<Segment> segments;

    public UriTemplate(String pattern) {
        this(pattern, "");
    }

    public UriTemplate(String pattern,
                       String base) {
        this.pattern = pattern;
        this.base = base;
        this.segments = new ArrayList<Segment>();

        StrTokenizer tokenizer = new StrTokenizer(pattern, '/');
        while (tokenizer.hasNext())
            segments.add(new Segment(tokenizer.nextToken()));
    }

    /**
     * Generates a URI relative to the template's base (or / if no base was
     * provided) by replacing the variable segments of the template with the
     * provided values. All literal segments, optional or no, are always
     * included. Values are bound into the template in the order in which they
     * are provided. If a value is not provided for an optional variable
     * segment, the segment is not included.
     *
     * @param values the (unescaped) values to be bound
     * @return a URI with variables replaced by bound values
     * @throws IllegalArgumentException if more or fewer values are
     * provided than are needed by the template or if a null value is
     * provided for a mandatory variable
     */
    public String bind(String... values) {
        StringBuffer buf = new StringBuffer();
        if (base != null)
            buf.append(base);
        buf.append("/");

        List<String> variables = Arrays.asList(values);
        Iterator<String> vi = variables.iterator();

        Iterator<Segment> si = segments.iterator();
        Segment segment = null;
        while (si.hasNext()) {
            segment = si.next();

            if (segment.isVariable()) {
                String value = null;
                if (vi.hasNext())
                    value = vi.next();
                if (value == null) {
                    if (segment.isOptional())
                        continue;
                    throw new IllegalArgumentException("Not enough values");
                }
                buf.append(escape(value));
            } else {
                buf.append(segment.getData());
            }

            if (si.hasNext() && vi.hasNext())
                buf.append("/");
        }

        if (vi.hasNext())
            if (segment.isAll()) {
                while (vi.hasNext())
                    buf.append(escape(vi.next()));
            } else
                throw new IllegalArgumentException("Too many values");

        return buf.toString();
    }

    /**
     * <p>
     * Matches a candidate uri-path against the template. Returns a
     * <code>Match</code> instance containing the names and values of
     * all variables found in the uri-path as specified by the
     * template.
     * </p>
     * <p>
     * Each literal segment in the template must match the
     * corresponding segment in the uri-path unless the segment is
     * optional. For each variable segment in the template, an entry
     * is added to the <code>Match</code> to be returned; the entry
     * key is the variable name from the template, and the entry value
     * is the corresponding (unescaped) token from the uri-path. If the
     * template includes an "all" segment, a match entry with key
     * <code>*</code> is also included containing the remainder of the
     * uri-path after the last matching segment.
     * </p>
     *
     * @param path the candidate uri-path
     * @return a <code>Match</code>, or <code>null</code> if the path
     * did not successfully match
     */
    public Match match(String path) {
        Match match = new Match();

        //if (log.isDebugEnabled())
            //log.debug("matching " + path + " to " + pattern);

        StrTokenizer candidate = new StrTokenizer(path, '/');
        Iterator<Segment> si = segments.iterator();

        Segment segment = null;
        while (si.hasNext() || segment.isAll()) {
            if (si.hasNext())
                segment = si.next();

            if (! candidate.hasNext()) {
                // if the segment is consuming all remaining data, then we're
                // done, since there is no more data
                if (segment.isAll())
                    break;
                // if the segment is optional, the candidate doesn't
                // have to have a matching segment
                if (segment.isOptional()) 
                    continue;
                // mandatory segment - not a match
                return null;
            }

            String token = candidate.nextToken();

            if (segment.isAll()) {
                String saved = match.get("*");
                if (saved == null)
                    saved = "";
                saved += "/" + unescape(token);
                match.put("*", saved);
            } else if (segment.isVariable())
                match.put(segment.getData(), unescape(token));
            else if (! segment.getData().equals(token))
                // literal segment doesn't match, so path is not a match
                return null;
        }

        if (candidate.hasNext() && ! segment.isAll())
            // candidate has more but our pattern is done
            return null;

        if (log.isDebugEnabled())
            log.debug("matched " + pattern);

        return match;
    }

    public String getPattern() {
        return pattern;
    }

    public String getBase() {
        return base;
    }

    public static final String escape(String raw) {
        try {
            // URLEncoder converts ' ' to '+', which is fine for HTML form
            // data but not for URLs
            String escaped = URLEncoder.encode(raw, "UTF-8");
            escaped = escaped.replace("+", "%20");
            return escaped;
        } catch (Exception e) {
            throw new RuntimeException("Could not escape string " + raw, e);
        }
    }

    public static final String escapePath(String path) {
        StringBuffer buf = new StringBuffer();
        StrTokenizer tokenizer = new StrTokenizer(path, '/');
        while (tokenizer.hasNext()) {
            buf.append(escape(tokenizer.nextToken()));
            if (tokenizer.hasNext())
                buf.append('/');
        }
        return buf.toString();
    }

    public static final String unescape(String escaped) {
        try {
            return URLDecoder.decode(escaped, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Could not unescape string " + escaped, e);
        }
    }

    private class Segment {
        private String data;
        private boolean variable = false;
        private boolean optional = false;
        private boolean all = false;

        public Segment(String data) {
            if (data.startsWith("{")) {
                if (data.endsWith("}?")) {
                    variable = true;
                    optional = true;
                    this.data = data.substring(1, data.length()-2);
                } else if (data.endsWith("}")) {
                    variable = true;
                    this.data = data.substring(1, data.length()-1);
                }
            } else if (data.endsWith("?")) {
                optional = true;
                this.data = data.substring(0, data.length()-1);
            } else if (data.equals("*")) {
                all = true;
            }

            if (this.data == null && ! all)
                this.data = data;
        }

        public String getData() {
            return data;
        }

        public boolean isVariable() {
            return variable;
        }

        public boolean isOptional() {
            return optional;
        }

        public boolean isAll() {
            return all;
        }
    }

    public class Match extends HashMap<String, String> {

        public String get(String key) {
            return super.get(key);
        }

        public String put(String key,
                          String value) {
            return super.put(key, value);
        }
    }
}
