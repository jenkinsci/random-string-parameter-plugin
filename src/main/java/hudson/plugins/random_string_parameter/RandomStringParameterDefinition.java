/*
 * The MIT License
 *
 * Copyright (c) 2012, Piotr Skotnicki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.random_string_parameter;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Random;

/**
 * String based parameter that supports setting a regular expression to validate the
 * user's entered value, giving real-time feedback on the value.
 * 
 * @author Piotr Skotnicki
 * @since 1.0
 * @see {@link ParameterDefinition}
 */
public class RandomStringParameterDefinition extends ParameterDefinition {

    private static final long serialVersionUID = 1L;
    private String failedValidationMessage;

    @DataBoundConstructor
    public RandomStringParameterDefinition(String name, String failedValidationMessage, String description) {
        super(name, description);
        this.failedValidationMessage = failedValidationMessage;
    }

    public RandomStringParameterDefinition(String name, String failedValidationMessage) {
        this(name, failedValidationMessage, null);
    }

    public String getFailedValidationMessage() {
        return failedValidationMessage;
    }

    public String getDefaultValue() {
        return createRandomString();
    }

    public String getRootUrl() {
        return Hudson.getInstance().getRootUrl();
    }
    
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static Random rnd = new Random();

    public String createRandomString() {
       StringBuilder sb = new StringBuilder(12);
       for(int i = 0; i < 12; ++i)
           sb.append(AB.charAt(rnd.nextInt(AB.length())));
       return sb.toString();
    }

    @Override
    public RandomStringParameterValue getDefaultParameterValue() {
        RandomStringParameterValue v = new RandomStringParameterValue(getName(), createRandomString(), getDescription());
        return v;
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        private String regex = "[a-zA-Z0-9_,-]{8,}";

        @Override
        public String getDisplayName() {
            return "Random String Parameter";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/random-string-parameter/help.html";
        }

        /**
         * Called to validate the passed user entered value against the configured regular expression.
         */
        public FormValidation doValidate(
                @QueryParameter("failedValidationMessage") final String failedValidationMessage,
                @QueryParameter("value") final String value) {
            try {
                if (Pattern.matches(regex, value)) {
                    return FormValidation.ok();
                } else {
                    return failedValidationMessage == null || "".equals(failedValidationMessage)
                            ? FormValidation.error("Value entered does not match regular expression: " + regex)
                            : FormValidation.error(failedValidationMessage);
                }
            } catch (PatternSyntaxException pse) {
                return FormValidation.error("Invalid regular expression [" + regex + "]: " + pse.getDescription());
            }
        }
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        RandomStringParameterValue value = req.bindJSON(RandomStringParameterValue.class, jo);
        value.setDescription(getDescription());
        return value;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        String[] value = req.getParameterValues(getName());
        if (value == null || value.length < 1) {
            return getDefaultParameterValue();
        } else {
            return new RandomStringParameterValue(getName(), value[0], getDescription());
        }
    }
}
