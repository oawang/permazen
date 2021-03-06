
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package io.permazen;

import io.permazen.annotation.JField;
import io.permazen.annotation.PermazenType;
import io.permazen.core.DeleteAction;
import io.permazen.test.TestSupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MetaAnnotationsTest extends TestSupport {

    @Test
    public void testGenerics1() throws Exception {
        final Permazen jdb = BasicTest.getPermazen(Mommy.class, Baby.class);
        final JTransaction jtx = jdb.createTransaction(true, ValidationMode.AUTOMATIC);
        JTransaction.setCurrent(jtx);
        try {

            final Mommy mommy = jtx.create(Mommy.class);
            final Baby baby1 = jtx.create(Baby.class);
            final Baby baby2 = jtx.create(Baby.class);

            baby1.setMommy(mommy);
            baby2.setMommy(mommy);

            try {
                jtx.validate();
                assert false;
            } catch (ValidationException e) {
                this.log.debug("got expected " + e);
            }

            baby2.setMommy(null);

            final Baby baby1copy = (Baby)baby1.cascadeCopyOut("load", false);
            Assert.assertNotNull(baby1copy.getMommy());

            jtx.commit();

        } finally {
            JTransaction.setCurrent(null);
        }
    }

// Meta-Annotations

    @PermazenType
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ModelClass {
    }

    @JField(onDelete = DeleteAction.DELETE, cascades = "load")
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Parent {

        @AliasFor(annotation = JField.class, attribute = "unique")
        boolean onlyChild() default false;
    }

// Model Classes

    @ModelClass
    public abstract static class Mommy implements JObject {
    }

    @ModelClass
    public abstract static class Baby implements JObject {

        @Parent(onlyChild = true)
        public abstract Mommy getMommy();
        public abstract void setMommy(Mommy mommy);
    }
}

