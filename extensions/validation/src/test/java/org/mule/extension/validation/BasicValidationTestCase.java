/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.extension.validation.internal.ValidationExtension.DEFAULT_LOCALE;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.MultipleValidationException;
import org.mule.extension.validation.api.MultipleValidationResult;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.api.Validator;
import org.mule.extension.validation.internal.ImmutableValidationResult;
import org.mule.extension.validation.internal.validator.CreditCardType;
import org.mule.mvel2.compiler.BlankLiteral;
import org.mule.transport.NullPayload;
import org.mule.util.ExceptionUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;

public class BasicValidationTestCase extends ValidationTestCase
{

    private String DOMAIN_NAME_255_CHARS = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345";
    private String DOMAIN_NAME_256_CHARS = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456";

    @Override
    protected String getConfigFile()
    {
        return "basic-validations.xml";
    }

    @Test
    public void domain() throws Exception
    {
        assertValid("domain", getTestEvent("mulesoft.com"));
        assertValid("domain", getTestEvent("mulesoft.COM"));
        //assertValid("domain", getTestEvent("mulesoft.com."));
        assertInvalid("domain", getTestEvent("xxx.yy"), messages.invalidDomain("xxx.yy"));
        assertInvalid("domain", getTestEvent(""), messages.invalidDomain(""));
        assertInvalid("domain", getTestEvent(null), messages.invalidDomain("null"));
        assertValid("domain", getTestEvent("1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa"));
        //assertValid("domain", getTestEvent("iesmartinezmonta�es.es"));
        //assertValid("domain", getTestEvent("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"));
        //assertValid("domain", getTestEvent(DOMAIN_NAME_255_CHARS));
        assertInvalid("domain", getTestEvent(DOMAIN_NAME_256_CHARS), messages.invalidDomain(DOMAIN_NAME_256_CHARS));
        assertInvalid("domain", getTestEvent(1), messages.invalidDomain(Long.toString(1)));
        //assertValid("domain", getTestEvent("localhost"));
        Object object = new Object();
        assertInvalid("domain", getTestEvent(object), messages.invalidDomain(object.toString()));
    }

    @Test
    public void topLevelDomain() throws Exception
    {
        List<String> domains = ImmutableList.of("com", "org", "net", "int", "edu", "gov", "mil", "arpa", "COM");
        for (String domain : domains)
        {
            assertValid("topLevelDomain", getTestEvent(domain));
        }
        assertInvalid("topLevelDomain", getTestEvent(""), messages.invalidTopLevelDomain(""));
        assertInvalid("topLevelDomain", getTestEvent("abc"), messages.invalidTopLevelDomain("abc"));
    }

    @Test
    public void toplevelDomainCountryCode() throws Exception
    {
        assertValid("toplevelDomainCountryCode", getTestEvent("ar"));
        assertValid("toplevelDomainCountryCode", getTestEvent("ac"));
        //assertValid("toplevelDomainCountryCode", getTestEvent("??")); // Russia domain
        //assertValid("toplevelDomainCountryCode", getTestEvent("xn--p1ai")); // ASCII DNS name for Russia domain
        assertInvalid("toplevelDomainCountryCode", getTestEvent("com"), messages.invalidDomainCountryCode("com"));
        assertInvalid("toplevelDomainCountryCode", getTestEvent("ppp"), messages.invalidDomainCountryCode("ppp"));
    }

    @Test
    public void creditCardNumber() throws Exception
    {
        assertValid("creditCardNumber", getTestEvent(VALID_CREDIT_CARD_NUMBER));
        assertInvalid("creditCardNumber", getTestEvent(INVALID_CREDIT_CARD_NUMBER), messages.invalidCreditCard("5555444433332222", CreditCardType.MASTERCARD));
        //assertValid("creditCardNumber", getTestEvent("4650970405116574"));
        //assertValid("creditCardNumber", getTestEvent("5310008936084612"));
        //assertValid("creditCardNumber", getTestEvent("6011639518666994"));
        //assertValid("creditCardNumber", getTestEvent("342396490153995"));
    }

    @Test
    public void email() throws Exception
    {
        assertValid("email", getTestEvent(VALID_EMAIL));
        assertValid("email", getTestEvent("a.b+c@mail.com"));
        assertValid("email", getTestEvent("a.b-c@mail.com"));
        assertValid("email", getTestEvent("\"a.@-c\"@mail.com"));
        //assertValid("email", getTestEvent("admin@mailserver1"));
        assertValid("email", getTestEvent("#!$%&'*+-/=?^_`{}|~@example.org"));
        assertValid("email", getTestEvent("\" \"@example.org"));
        //assertValid("email", getTestEvent("�������@example.com"));
        //assertValid("email", getTestEvent("�������@�������.com"));
        assertInvalid("email", getTestEvent(INVALID_EMAIL), messages.invalidEmail("@mulesoft.com"));


        List<String> invalidEmails = ImmutableList.of(
                "Abc.example.com",
                "A@b@c@example.com",
                "john..doe@example.com",
                "john.doe@example..com"
                //, " a@a.com",
                //"a@a.com "
        );
        for (String invalidEmail : invalidEmails)
        {
            assertInvalid("email", getTestEvent(invalidEmail), messages.invalidEmail(invalidEmail));
        }

    }

    @Test
    public void ip() throws Exception
    {
        assertValid("ip", getTestEvent("127.0.0.1"));
        assertValid("ip", getTestEvent("0.0.0.0"));
        assertValid("ip", getTestEvent("0.0.0.1"));
        assertValid("ip", getTestEvent("10.0.0.0"));
        assertValid("ip", getTestEvent("192.168.0.0"));
        assertValid("ip", getTestEvent("172.16.0.0"));
        //assertValid("ip", getTestEvent("2001:0db8:85a3:0042:1000:8a2e:0370:7334"));
        assertInvalid("ip", getTestEvent("1.1.256.0"), messages.invalidIp("1.1.256.0"));
        assertInvalid("ip", getTestEvent("0.0.0.a"), messages.invalidIp("0.0.0.a"));
        assertInvalid("ip", getTestEvent("12.1.2"), messages.invalidIp("12.1.2"));
        assertInvalid("ip", getTestEvent("12.1.2."), messages.invalidIp("12.1.2."));
        assertInvalid("ip", getTestEvent("192.168.100.0/24"), messages.invalidIp("192.168.100.0/24"));
        assertInvalid("ip", getTestEvent(0), messages.invalidIp("0"));
    }

    @Test
    public void isbn10() throws Exception
    {
        assertValid("isbn10", getTestEvent(VALID_ISBN10));
        assertValid("isbn10", getTestEvent("1566199093"));
        //assertValid("isbn10", getTestEvent("3-04-013341-X")); // X = 10
        assertInvalid("isbn10", getTestEvent(INVALID_ISBN10), messages.invalidISBN10("88"));
    }

    @Test
    public void isbn13() throws Exception
    {
        assertValid("isbn13", getTestEvent(VALID_ISBN13));
        assertInvalid("isbn13", getTestEvent(INVALID_ISBN13), messages.invalidISBN13("88"));
    }

    @Test
    public void url() throws Exception
    {
        assertValid("url", getTestEvent(VALID_URL));
        assertValid("url", getTestEvent("https://www.example.com/foo/?bar=baz&inga=42&quux"));
        //assertValid("url", getTestEvent("http://?df.ws/123"));
        //assertValid("url", getTestEvent("foo://username:password@example.com:8042/over/there/index.dtb?type=animal&name=narwhal#nose"));
        assertValid("url", getTestEvent("http://foo.com/blah_(wikipedia)_blah#cite-1"));
        //assertValid("url", getTestEvent("http://userid:password@example.com:8080/"));
        assertValid("url", getTestEvent("https://freewebs.com/accumsan/odio/curabitur/convallis/duis/consequat.xml?dis=hac&parturient=habitasse&montes=platea&nascetur=dictumst&ridiculus=morbi&mus=vestibulum&etiam=velit&vel=id&augue=pretium&vestibulum=iaculis&rutrum=diam&rutrum=erat&neque=fermentum&aenean=justo&auctor=nec&gravida=condimentum&sem=neque&praesent=sapien&id=placerat&massa=ante&id=nulla&nisl=justo&venenatis=aliquam&lacinia=quis&aenean=turpis&sit=eget&amet=elit&justo=sodales&morbi=scelerisque&ut=mauris&odio=sit&cras=amet&mi=eros&pede=suspendisse&malesuada=accumsan&in=tortor&imperdiet=quis&et=turpis&commodo=sed&vulputate=ante&justo=vivamus&in=tortor&blandit=duis&ultrices=mattis&enim=egestas&lorem=metus&ipsum=aenean&dolor=fermentum&sit=donec&amet=ut&consectetuer=mauris&adipiscing=eget&elit=massa&proin=tempor&interdum=convallis&mauris=nulla&non=neque&ligula=libero&pellentesque=convallis&ultrices=eget&phasellus=eleifend&id=luctus&sapien=ultricies&in=eu&sapien=nibh&iaculis=quisque&congue=id&vivamus=justo&metus=sit&arcu=amet&adipiscing=sapien&molestie=dignissim&hendrerit=vestibulum&at=vestibulum&vulputate=ante&vitae=ipsum&nisl=primis&aenean=in&lectus=faucibus&pellentesque=orci&eget=luctus&nunc=et&donec=ultrices&quis=posuere&orci=cubilia&eget=curae&orci=nulla&vehicula=dapibus&condimentum=dolor,Alan,Kelly,akelly3@globo.com,China,255.132.0.180"));
        assertInvalid("url", getTestEvent(INVALID_URL), messages.invalidUrl("here"));
        assertInvalid("url", getTestEvent(""), messages.invalidUrl(""));
        assertInvalid("url", getTestEvent("http://"), messages.invalidUrl("http://"));
        //assertInvalid("url", getTestEvent("http://hola.com "), messages.invalidUrl("http://hola.com "));
        //assertInvalid("url", getTestEvent("http://hola. com"), messages.invalidUrl("http://hola. com"));
        assertInvalid("url", getTestEvent("hola.com"), messages.invalidUrl("hola.com"));
    }

    @Test
    public void time() throws Exception
    {
        String locale = Locale.getDefault().getLanguage();
        Map<String, String> values = new HashMap<>();
        values.put("h:mm a", "12:08 PM");
        values.put("yyyy.MM.dd G 'at' HH:mm:ss z", "1960.07.04 AD at 12:08:56 PDT");
        values.put("EEE, MMM d, ''yy", "Wed, Jul 4, '01");
        values.put("hh 'o''clock' a, zzzz", "12 o'clock PM, Pacific Daylight Time");
        values.put("yyyyy.MMMMM.dd GGG hh:mm aaa", "02001.July.04 AD 12:08 PM");
        values.put("EEE, d MMM yyyy HH:mm:ss Z", "Wed, 4 Jul 2001 12:08:56 -0700");
        values.put("K:mm a, z", "0:08 PM, PDT");

        for (Map.Entry<String, String> entry : values.entrySet())
        {
            assertValidTime(entry.getKey(), locale, entry.getValue());
            assertInvalidTime(entry.getKey(), Locale.CHINESE.getLanguage(), entry.getValue());
        }

        values.clear();
        values.put("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "2001-07-04T12:08:56.235-0700");
        values.put("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "2001-07-04T12:08:56.235-07:00");
        values.put("yyMMddHHmmssZ", "010704120856-0700");
        values.put("YYYY-'W'ww-u", "2001-W27-3");
        for (Map.Entry<String, String> entry : values.entrySet())
        {
            assertValidTime(entry.getKey(), locale, entry.getValue());
        }

        final String invalidPattern = "yyMMddHHmmssZ";
        MuleEvent event = getTestEvent("12:08 PM");
        event.setFlowVariable("pattern", invalidPattern);
        event.setFlowVariable("locale", locale);
        assertInvalid("time", event, messages.invalidTime("12:08 PM", DEFAULT_LOCALE, invalidPattern));
    }

    private void assertInvalidTime(String pattern, String locale, String string) throws Exception
    {
        MuleEvent event = getTestEvent(string);
        event.setFlowVariable("pattern", pattern);
        event.setFlowVariable("locale", locale);
        assertInvalid("time", event, messages.invalidTime(string, locale, pattern));
    }

    private void assertValidTime(String pattern, String locale, String string) throws Exception
    {
        MuleEvent event = getTestEvent(string);
        event.setFlowVariable("pattern", pattern);
        event.setFlowVariable("locale", locale);
        assertValid("time", event);
    }

    private void assertValidRegex(String regex, String value, boolean caseSensitive) throws Exception
    {
        MuleEvent event = getTestEvent(value);
        event.setFlowVariable("regexp", regex);
        event.setFlowVariable("caseSensitive", caseSensitive);
        assertValid("matchesRegex", event);
    }

    private void assertInvalidRegex(String regex, String value, boolean caseSensitive) throws Exception
    {
        MuleEvent event = getTestEvent(value);
        event.setFlowVariable("regexp", regex);
        event.setFlowVariable("caseSensitive", caseSensitive);
        assertInvalid("matchesRegex", event, messages.regexDoesNotMatch(value, regex));
    }

    @Test
    public void matchesRegex() throws Exception
    {
        assertValidRegex("[tT]rue", "true", false);
        assertValidRegex("[tT]rue", "true", true);
        assertValidRegex("[tT]rue", "TRUE", false);
        assertValidRegex("(a.*b)(c)(d)", "alafjajbcd", true);
        assertValidRegex("�", "�", true);
        assertInvalidRegex("[tT]rue", "TRUE", true);
        assertInvalidRegex("[tT]rue", "tTrue", false);
        assertInvalidRegex("[tT]rue", "", false);
        assertInvalidRegex("[tT]rue", " ", false);
        assertInvalidRegex("[tT]rue", "[tT]rue", false);
    }


    @Test(expected = ValidationException.class)
    public void flowFailsWithNullValue() throws Throwable
    {
        try
        {
            MuleEvent event = getTestEvent(null);
            event.setFlowVariable("regexp", ".*");
            event.setFlowVariable("caseSensitive", "false");
            runFlow("matchesRegex", event);
        }
        catch (Exception e)
        {
            throw ExceptionUtils.getRootCause(e);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void flowFailsWithNullRegex() throws Throwable
    {
        try
        {
            MuleEvent event = getTestEvent("a");
            event.setFlowVariable("regexp", null);
            event.setFlowVariable("caseSensitive", "false");
            runFlow("matchesRegex", event);
        }
        catch (Exception e)
        {
            throw ExceptionUtils.getRootCause(e);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void flowFailsWithEmptyRegex() throws Throwable
    {
        try
        {
            MuleEvent event = getTestEvent("a");
            event.setFlowVariable("regexp", "");
            event.setFlowVariable("caseSensitive", "false");
            runFlow("matchesRegex", event);
        }
        catch (Exception e)
        {
            throw ExceptionUtils.getRootCause(e);
        }
    }

    @Test
    public void size() throws Exception
    {
        assertSize("abc");
        assertSize(Arrays.asList("a", "b", "c"));
        assertSize(new String[] {"a", "b", "c"});
        assertSize(ImmutableMap.of("a", 1, "b", 2, "c", 3));
    }

    @Test
    public void otherIsLong() throws Exception
    {
        assertIsNumber("400%", "###%", Locale.US.toString(), null, null, "wrong pattern", ArrayIndexOutOfBoundsException.class);
        //assertIsNumber("400%", "###%", null, null, null, "wrong pattern", ArrayIndexOutOfBoundsException.class);
        assertIsNumber("400%", "###%", "", null, null, "wrong pattern", ArrayIndexOutOfBoundsException.class);
        //assertIsNumber("420%", "###%", "", "4", "5", "wrong pattern", ArrayIndexOutOfBoundsException.class);
        //assertIsNumberFails("620%", "###%", "", "4", "5", "wrong pattern", ArrayIndexOutOfBoundsException.class);
        assertIsNumberFails("6", "###%", "", null, null, "wrong pattern", ArrayIndexOutOfBoundsException.class);
    }

    private void assertIsNumberFails(String value, String pattern, String locale, Object min, Object max, String message, Class<? extends Throwable> exception) throws Exception
    {
        try
        {
            assertIsNumber(value, pattern, locale, min, max, message, exception);
            fail();
        }
        catch (Throwable e)
        {
            assertThat(e, Matchers.instanceOf(MessagingException.class));
            assertThat(e.getCause(), Matchers.instanceOf(exception));
            assertThat(e.getCause().getMessage(), Matchers.equalTo(message));
        }

    }

    private void assertIsNumber(String value, String pattern, String locale, Object min, Object max, String message, Class<? extends Throwable> exception) throws Exception
    {
        MuleEvent event = getTestEvent(value);
        event.setFlowVariable("pattern", pattern);
        event.setFlowVariable("locale", locale);
        event.setFlowVariable("minValue", min);
        event.setFlowVariable("maxValue", max);
        event.setFlowVariable("exceptionMessage", message);
        event.setFlowVariable("exceptionClass", exception);
        runFlow("full-long", event);
    }

    @Test
    public void isLong() throws Exception
    {
        assertNumberValue("long", Long.class, Long.MAX_VALUE / 2, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Test
    public void isInteger() throws Exception
    {
        assertNumberValue("integer", Integer.class, Integer.MAX_VALUE / 2, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void isShort() throws Exception
    {
        assertNumberValue("short", Short.class, new Short("100"), new Integer(Short.MIN_VALUE + 1).shortValue(), new Integer(Short.MAX_VALUE - 1).shortValue(), Short.MIN_VALUE, Short.MAX_VALUE);
    }

    @Test
    public void isDouble() throws Exception
    {
        assertNumberValue("double", Double.class, 10D, 1D, 10D, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    @Test
    public void isFloat() throws Exception
    {
        assertNumberValue("float", Float.class, 10F, 1F, 10F, 0F, 20F);
    }

    @Test
    public void isTrue() throws Exception
    {
        assertValid("isTrue", getTestEvent(true));
        assertInvalid("isTrue", getTestEvent(false), messages.failedBooleanValidation(false, true));
        assertInvalid("isTrue", getTestEvent(Boolean.FALSE), messages.failedBooleanValidation(false, true));
        assertInvalid("isTrue", getTestEvent(null), IllegalArgumentException.class);
        assertInvalid("isTrue", getTestEvent(new Object()), IllegalArgumentException.class);
        assertInvalid("isTrue", getTestEvent(1), IllegalArgumentException.class);
        assertInvalid("isTrue", getTestEvent(NullPayload.getInstance()), IllegalArgumentException.class);
        //assertInvalid("isTrue", getTestEvent("hello"), IllegalArgumentException.class);
    }

    @Test
    public void isFalse() throws Exception
    {
        assertValid("isFalse", getTestEvent(false));
        assertInvalid("isFalse", getTestEvent(true), messages.failedBooleanValidation(true, false));
        assertInvalid("isFalse", getTestEvent(Boolean.TRUE), messages.failedBooleanValidation(true, false));
        assertInvalid("isFalse", getTestEvent(null), IllegalArgumentException.class);
        assertInvalid("isFalse", getTestEvent(new Object()), IllegalArgumentException.class);
        assertInvalid("isFalse", getTestEvent(1), IllegalArgumentException.class);
        assertInvalid("isFalse", getTestEvent(NullPayload.getInstance()), IllegalArgumentException.class);
        //assertInvalid("isFalse", getTestEvent("hello"), IllegalArgumentException.class);
    }

    @Test
    public void notEmpty() throws Exception
    {
        final String flowName = "notEmpty";

        assertValid(flowName, getTestEvent("a"));
        assertValid(flowName, getTestEvent(Collections.singletonList("a")));
        assertValid(flowName, getTestEvent(new String[] {"a"}));
        assertValid(flowName, getTestEvent(ImmutableMap.of("a", "A")));

        assertInvalid(flowName, getTestEvent(null), messages.valueIsNull());
        assertInvalid(flowName, getTestEvent(""), messages.stringIsBlank());
        assertInvalid(flowName, getTestEvent(ImmutableList.of()), messages.collectionIsEmpty());
        assertInvalid(flowName, getTestEvent(new String[] {}), messages.arrayIsEmpty());
        assertInvalid(flowName, getTestEvent(new Object[] {}), messages.arrayIsEmpty());
        assertInvalid(flowName, getTestEvent(new int[] {}), messages.arrayIsEmpty());
        assertInvalid(flowName, getTestEvent(ImmutableMap.of()), messages.mapIsEmpty());
        assertInvalid(flowName, getTestEvent(BlankLiteral.INSTANCE), messages.valueIsBlankLiteral());
    }

    @Test
    public void empty() throws Exception
    {
        final String flowName = "empty";

        assertValid(flowName, getTestEvent(""));
        assertValid(flowName, getTestEvent(""));
        assertValid(flowName, getTestEvent(ImmutableList.of()));
        assertValid(flowName, getTestEvent(new String[] {}));
        assertValid(flowName, getTestEvent(ImmutableMap.of()));

        assertInvalid(flowName, getTestEvent("a"), messages.stringIsNotBlank());
        assertInvalid(flowName, getTestEvent(Collections.singletonList("a")), messages.collectionIsNotEmpty());
        assertInvalid(flowName, getTestEvent(new String[] {"a"}), messages.arrayIsNotEmpty());
        assertInvalid(flowName, getTestEvent(new Object[] {new Object()}), messages.arrayIsNotEmpty());
        assertInvalid(flowName, getTestEvent(new int[] {0}), messages.arrayIsNotEmpty());
        assertInvalid(flowName, getTestEvent(ImmutableMap.of("a", "a")), messages.mapIsNotEmpty());
    }

    @Test
    public void successfulAll() throws Exception
    {
        MuleEvent responseEvent = runFlow("all", getAllEvent(VALID_EMAIL, VALID_CREDIT_CARD_NUMBER, true));
        assertThat(responseEvent.getMessage().getPayload(), is(instanceOf(MultipleValidationResult.class)));
        MultipleValidationResult result = (MultipleValidationResult) responseEvent.getMessage().getPayload();
        assertThat(result.isError(), is(false));
        assertThat(result.getFailedValidationResults(), hasSize(0));
    }

    @Test
    public void oneFailureInAllWithoutException() throws Exception
    {
        MuleEvent responseEvent = runFlow("all", getAllEvent(INVALID_EMAIL, VALID_CREDIT_CARD_NUMBER, false));
        assertThat(responseEvent.getMessage().getPayload(), is(instanceOf(MultipleValidationResult.class)));
        MultipleValidationResult result = (MultipleValidationResult) responseEvent.getMessage().getPayload();
        assertThat(result.isError(), is(true));
        assertThat(result.getMessage(), is(messages.invalidEmail(INVALID_EMAIL).getMessage()));
        assertThat(result.getFailedValidationResults(), hasSize(1));
        assertThat(result.getFailedValidationResults().get(0).isError(), is(true));
    }

    @Test
    public void twoFailureInAllWithoutException() throws Exception
    {
        MuleEvent responseEvent = runFlow("all", getAllEvent(INVALID_EMAIL, INVALID_CREDIT_CARD_NUMBER, false));
        assertThat(responseEvent.getMessage().getPayload(), is(instanceOf(ValidationResult.class)));
        MultipleValidationResult result = (MultipleValidationResult) responseEvent.getMessage().getPayload();
        assertThat(result.isError(), is(true));

        String expectedMessage = Joiner.on('\n').join(messages.invalidCreditCard(INVALID_CREDIT_CARD_NUMBER, CreditCardType.MASTERCARD),
                                                      messages.invalidEmail(INVALID_EMAIL));

        assertThat(result.getMessage(), is(expectedMessage));

        for (ValidationResult failedValidationResult : result.getFailedValidationResults())
        {
            assertThat(failedValidationResult.isError(), is(true));
        }
    }

    @Test
    public void failureInAllThrowsException() throws Exception
    {
        try
        {
            runFlow("all", getAllEvent(INVALID_EMAIL, VALID_CREDIT_CARD_NUMBER, true));
            fail("was expecting a failure");
        }
        catch (Exception e)
        {
            Throwable root = ExceptionUtils.getRootCause(e);
            assertThat(root, is(instanceOf(MultipleValidationException.class)));
            MultipleValidationResult result = ((MultipleValidationException) root).getMultipleValidationResult();
            assertThat(result.getFailedValidationResults(), hasSize(1));
            assertThat(result.isError(), is(true));
            assertThat(result.getMessage(), is(messages.invalidEmail(INVALID_EMAIL).getMessage()));
        }
    }

    @Test
    public void customValidationByClass() throws Exception
    {
        assertCustomValidator("customValidationByClass");
    }

    @Test
    public void customValidationByRef() throws Exception
    {
        assertCustomValidator("customValidationByRef");
    }

    @Test
    public void customSuccessfulValidator() throws Exception
    {
        MuleEvent result = runFlow("customValidationSuccess", "hello");
        assertThat(result.getMessage().getPayload(), CoreMatchers.<Object>equalTo("hello"));
    }

    @Test
    public void customFailingValidator() throws Exception
    {
        try
        {
            runFlow("customValidationFailure", "hello");
            //fail();
        }
        catch (MessagingException e)
        {
            assertThat(e.getCause(), is(instanceOf(ValidationException.class)));
            assertThat(e.getCause().getMessage(), equalTo(TEST_MESSAGE));
        }
    }

    private void assertCustomValidator(String flowName) throws Exception
    {
        try
        {
            runFlow(flowName, getTestEvent(""));
            fail("was expecting a failure");
        }
        catch (Exception e)
        {
            Throwable cause = ExceptionUtils.getRootCause(e);
            assertThat(CUSTOM_VALIDATOR_EXCEPTION, is(sameInstance(cause)));
        }
    }

    private MuleEvent getAllEvent(String email, String creditCardNumber, boolean throwsException) throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("creditCardNumber", creditCardNumber);
        event.setFlowVariable("email", email);
        event.setFlowVariable("throwsException", throwsException);

        return event;
    }

    private <T extends Number> void assertNumberValue(String flowName,
                                                      Class<T> numberType,
                                                      T value,
                                                      T minValue,
                                                      T maxValue,
                                                      T lowerBoundaryViolation,
                                                      T upperBoundaryViolation) throws Exception
    {
        assertValid(flowName, getNumberValidationEvent(value, minValue, maxValue));
        final String invalid = "unparseable";
        assertInvalid(flowName, getNumberValidationEvent(invalid, minValue, maxValue), messages.invalidNumberType(invalid, numberType));

        assertInvalid(flowName, getNumberValidationEvent(upperBoundaryViolation, minValue, maxValue), messages.greaterThan(upperBoundaryViolation, maxValue));
        assertInvalid(flowName, getNumberValidationEvent(lowerBoundaryViolation, minValue, maxValue), messages.lowerThan(lowerBoundaryViolation, minValue));
    }

    private void assertSize(Object value) throws Exception
    {
        final String flowName = "size";
        final int expectedSize = 3;
        int minLength = 0;
        int maxLength = 3;

        assertValid(flowName, getSizeValidationEvent(value, minLength, maxLength));
        minLength = 3;
        assertValid(flowName, getSizeValidationEvent(value, minLength, maxLength));

        maxLength = 2;
        assertInvalid(flowName, getSizeValidationEvent(value, minLength, maxLength), messages.greaterThanMaxSize(value, maxLength, expectedSize));

        minLength = 5;
        maxLength = 10;
        assertInvalid(flowName, getSizeValidationEvent(value, minLength, maxLength), messages.lowerThanMinSize(value, minLength, expectedSize));
    }

    private MuleEvent getSizeValidationEvent(Object value, int minLength, int maxLength) throws Exception
    {
        MuleEvent event = getTestEvent(value);
        event.setFlowVariable("minLength", minLength);
        event.setFlowVariable("maxLength", maxLength);

        return event;
    }

    private MuleEvent getNumberValidationEvent(Object value, Object minValue, Object maxValue) throws Exception
    {
        MuleEvent event = getTestEvent(value);
        event.setFlowVariable("minValue", minValue);
        event.setFlowVariable("maxValue", maxValue);

        return event;
    }

    private void assertValid(String flowName, MuleEvent event) throws Exception
    {
        MuleEvent responseEvent = runFlow(flowName, event);
        assertThat(responseEvent.getMessage().getExceptionPayload(), is(nullValue()));
    }

    @Override
    protected boolean isStartContext()
    {
        return super.isStartContext();
    }

    private void assertInvalid(String flowName, MuleEvent event, Class<? extends Throwable> type) throws Exception
    {
        try
        {
            runFlow(flowName, event);
            fail("Was expecting a failure");
        }
        catch (Exception e)
        {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            assertThat(rootCause, is(instanceOf(type)));
        }
    }

    private void assertInvalid(String flowName, MuleEvent event, Message expectedMessage) throws Exception
    {
        try
        {
            runFlow(flowName, event);
            fail("Was expecting a failure");
        }
        catch (Exception e)
        {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            assertThat(rootCause, is(instanceOf(ValidationException.class)));
            assertThat(rootCause.getMessage(), is(expectedMessage.getMessage()));
            // assert that all placeholders were replaced in message
            assertThat(rootCause.getMessage(), not(containsString("${")));
        }
    }

    public static class SuccessfulCustomValidator implements Validator
    {

        @Override
        public ValidationResult validate(MuleEvent event)
        {
            return ImmutableValidationResult.ok();
        }
    }

    public static class FailingCustomValidator implements Validator
    {

        @Override
        public ValidationResult validate(MuleEvent event)
        {
            return ImmutableValidationResult.error(TEST_MESSAGE);
        }
    }

    public static class TestCustomValidator implements Validator
    {

        @Override
        public ValidationResult validate(MuleEvent event)
        {
            throw CUSTOM_VALIDATOR_EXCEPTION;
        }
    }
}
