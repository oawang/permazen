
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb;

import java.util.Arrays;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.jsimpledb.annotation.JField;
import org.jsimpledb.annotation.JSimpleClass;
import org.jsimpledb.annotation.OnVersionChange;
import org.jsimpledb.core.Database;
import org.jsimpledb.core.EnumValue;
import org.jsimpledb.core.ObjId;
import org.jsimpledb.kv.simple.SimpleKVDatabase;
import org.testng.Assert;
import org.testng.annotations.Test;

public class OnVersionChangeTest extends TestSupport {

    @Test
    public void testOnVersionChange() {

        final SimpleKVDatabase kvstore = new SimpleKVDatabase();
        final Database db = new Database(kvstore);

        ObjId id1;
        ObjId id2;
        ObjId id3;
        ObjId id4;
        ObjId id5;

    // Version 1

        JSimpleDB jdb = new JSimpleDB(db, 1, Arrays.<Class<?>>asList(Person1.class));
        JTransaction tx = jdb.createTransaction(true, ValidationMode.AUTOMATIC);
        JTransaction.setCurrent(tx);
        try {

            Person1 p1 = tx.create(Person1.class);
            Person1 p2 = tx.create(Person1.class);
            Person1 p3 = tx.create(Person1.class);
            Person1 p4 = tx.create(Person1.class);
            Person1 p5 = tx.create(Person1.class);

            p1.setIndex(1);
            p2.setIndex(2);
            p3.setIndex(3);
            p4.setIndex(4);
            p5.setIndex(5);

            id1 = p1.getObjId();
            id2 = p2.getObjId();
            id3 = p3.getObjId();
            id4 = p4.getObjId();
            id5 = p5.getObjId();

            p1.setName("Smith, Joe");
            p2.setName("Jones, Fred");
            p3.setName("Brown, Kelly");

            p1.setFriend(p2);
            p2.setFriend(p3);

            p1.setEnum1(Enum1.AAA);
            p2.setEnum1(Enum1.BBB);
            p3.setEnum1(Enum1.CCC);
            p4.setEnum1(Enum1.DDD);
            p5.setEnum1(Enum1.EEE);

            Assert.assertEquals(p1.getSchemaVersion(), 1);
            Assert.assertEquals(tx.queryVersion(), buildMap(
              1, buildSet(p1, p2, p3, p4, p5)));

            tx.commit();

        } finally {
            JTransaction.setCurrent(null);
        }

    // Version 2

        jdb = new JSimpleDB(db, 2, Arrays.<Class<?>>asList(Person2.class));
        tx = jdb.createTransaction(true, ValidationMode.AUTOMATIC);
        JTransaction.setCurrent(tx);
        try {

            final Person2 p1 = jdb.getJObject(id1, Person2.class);
            final Person2 p2 = jdb.getJObject(id2, Person2.class);
            final Person2 p3 = jdb.getJObject(id3, Person2.class);
            final Person2 p4 = jdb.getJObject(id4, Person2.class);
            final Person2 p5 = jdb.getJObject(id5, Person2.class);

            Assert.assertEquals(tx.queryVersion(), buildMap(
              1, buildSet(p1, p2, p3, p4, p5)));

            Assert.assertEquals(p1.getSchemaVersion(), 1);
            Assert.assertEquals(p2.getSchemaVersion(), 1);
            Assert.assertEquals(p3.getSchemaVersion(), 1);
            Assert.assertEquals(p4.getSchemaVersion(), 1);
            Assert.assertEquals(p5.getSchemaVersion(), 1);

            p1.upgrade();
            p2.upgrade();
            p3.upgrade();

            Assert.assertEquals(p1.getSchemaVersion(), 2);
            Assert.assertEquals(p2.getSchemaVersion(), 2);
            Assert.assertEquals(p3.getSchemaVersion(), 2);
            Assert.assertEquals(p4.getSchemaVersion(), 1);
            Assert.assertEquals(p5.getSchemaVersion(), 1);

            Assert.assertEquals(p1.getLastName(), "Smith");
            Assert.assertEquals(p1.getFirstName(), "Joe");

            Assert.assertEquals(tx.queryVersion(), buildMap(
              1, buildSet(p4, p5),
              2, buildSet(p1, p2, p3)));

            Assert.assertEquals(p2.getLastName(), "Jones");
            Assert.assertEquals(p2.getFirstName(), "Fred");

            Assert.assertEquals(p3.getLastName(), "Brown");
            Assert.assertEquals(p3.getFirstName(), "Kelly");

            Assert.assertEquals(p1.getAge(), 0);
            Assert.assertEquals(p2.getAge(), 0);
            Assert.assertEquals(p3.getAge(), 0);

            Assert.assertSame(p1.getEnum2(), Enum2.CCC);
            Assert.assertSame(p2.getEnum2(), Enum2.BBB);
            Assert.assertSame(p3.getEnum2(), Enum2.CCC);
            Assert.assertSame(p4.getEnum2(), Enum2.DDD);
            Assert.assertSame(p5.getEnum2(), null);

            Assert.assertEquals(tx.queryVersion(), buildMap(
              2, buildSet(p1, p2, p3, p4, p5)));

            p1.setAge(10);
            p2.setAge(20);
            p3.setAge(30);

            tx.commit();

        } finally {
            JTransaction.setCurrent(null);
        }

    // Version 3

        jdb = new JSimpleDB(db, 3, Arrays.<Class<?>>asList(Person3.class));
        tx = jdb.createTransaction(true, ValidationMode.AUTOMATIC);
        JTransaction.setCurrent(tx);
        try {

            final Person3 p1 = jdb.getJObject(id1, Person3.class);
            final Person3 p2 = jdb.getJObject(id2, Person3.class);
            final Person3 p3 = jdb.getJObject(id3, Person3.class);
            final Person3 p4 = jdb.getJObject(id4, Person3.class);
            final Person3 p5 = jdb.getJObject(id5, Person3.class);

            Assert.assertEquals(tx.queryVersion(), buildMap(
              2, buildSet(p1, p2, p3, p4, p5)));

            Assert.assertEquals(p1.getAge(), 10.0f);
            Assert.assertEquals(p2.getAge(), 20.0f);
            Assert.assertEquals(p3.getAge(), 30.0f);

            Assert.assertEquals(tx.queryVersion(), buildMap(
              2, buildSet(p4, p5),
              3, buildSet(p1, p2, p3)));

            tx.commit();

        } finally {
            JTransaction.setCurrent(null);
        }

    // Version 4

        jdb = new JSimpleDB(db, 4, Arrays.<Class<?>>asList(Person4.class, Name.class));
        tx = jdb.createTransaction(true, ValidationMode.AUTOMATIC);
        JTransaction.setCurrent(tx);
        try {

            final Person4 p1 = jdb.getJObject(id1, Person4.class);
            final Person4 p2 = jdb.getJObject(id2, Person4.class);
            final Person4 p3 = jdb.getJObject(id3, Person4.class);
            final Person4 p4 = jdb.getJObject(id4, Person4.class);
            final Person4 p5 = jdb.getJObject(id5, Person4.class);

            Assert.assertEquals(tx.queryVersion(), buildMap(
              2, buildSet(p4, p5),
              3, buildSet(p1, p2, p3)));

            Assert.assertEquals(p1.getName().getLastName(), "Smith");
            Assert.assertEquals(p1.getName().getFirstName(), "Joe");
            Assert.assertEquals(p2.getName().getLastName(), "Jones");
            Assert.assertEquals(p2.getName().getFirstName(), "Fred");
            Assert.assertEquals(p3.getName().getLastName(), "Brown");
            Assert.assertEquals(p3.getName().getFirstName(), "Kelly");

            Assert.assertEquals(p1.getAge(), 10.0f);
            Assert.assertEquals(p2.getAge(), 20.0f);
            Assert.assertEquals(p3.getAge(), 30.0f);

            Assert.assertEquals(tx.queryVersion(), buildMap(
              2, buildSet(p4, p5),
              4, buildSet(p1, p2, p3, p1.getName(), p2.getName(), p3.getName())));

            tx.commit();

        } finally {
            JTransaction.setCurrent(null);
        }

    }

// HasName

    public interface HasName {
        String getLastName();
        String getFirstName();
    }

// Version 1

    public enum Enum1 {
        AAA,    // 0
        BBB,    // 1
        CCC,    // 2
        DDD,    // 3
        EEE;    // 4
    }

    @JSimpleClass(storageId = 100)
    public abstract static class Person1 implements JObject {

        @JField(storageId = 97)
        public abstract int getIndex();
        public abstract void setIndex(int index);

        @JField(storageId = 98)
        public abstract Enum1 getEnum1();
        public abstract void setEnum1(Enum1 enum1);

        @JField(storageId = 99)
        public abstract Person1 getFriend();
        public abstract void setFriend(Person1 friend);

        @JField(storageId = 101)
        public abstract String getName();
        public abstract void setName(String lastName);
    }

// Version 2

    public enum Enum2 {
        CCC,    // 0
        BBB,    // 1
        DDD;    // 2
    }

    @JSimpleClass(storageId = 100)
    public abstract static class Person2 implements JObject, HasName {

        @JField(storageId = 97)
        public abstract int getIndex();
        public abstract void setIndex(int index);

        @JField(storageId = 98)
        public abstract Enum2 getEnum2();
        public abstract void setEnum2(Enum2 enum2);

        @JField(storageId = 101, indexed = true)
        @Override
        public abstract String getLastName();
        public abstract void setLastName(String lastName);

        @JField(storageId = 102)
        @Override
        public abstract String getFirstName();
        public abstract void setFirstName(String firstName);

        @JField(storageId = 103)
        public abstract int getAge();
        public abstract void setAge(int age);

        @OnVersionChange
        private void versionChange(int oldVersion, int newVersion, Map<Integer, Object> oldValues) {
            assert oldVersion == 1;
            assert newVersion == 2;

            // Verify enum old value
            final int index = this.getIndex();
            Assert.assertEquals(oldValues.get(98), new EnumValue(Enum1.values()[index - 1]), "wrong enum for index " + index);

            // Get old name
            final String name = (String)oldValues.get(101);
            if (name == null)
                return;

            // Update name
            final int comma = name.indexOf(',');
            this.setLastName(name.substring(0, comma).trim());
            this.setFirstName(name.substring(comma + 1).trim());
            if (this.getLastName().equals("Smith")) {
                final Person2 oldFriend = (Person2)oldValues.get(99);
                Assert.assertEquals(oldFriend.getLastName(), "Jones");
            } else if (this.getLastName().equals("Jones")) {
                final Person2 oldFriend = (Person2)oldValues.get(99);
                Assert.assertEquals(oldFriend.getLastName(), "Brown");
            } else
                Assert.assertNull(oldValues.get(99));
        }
    }

// Version 3

    @JSimpleClass(storageId = 100)
    public abstract static class Person3 implements JObject, HasName {

        @JField(storageId = 101, indexed = true)
        @Override
        public abstract String getLastName();
        public abstract void setLastName(String lastName);

        @JField(storageId = 102)
        @Override
        public abstract String getFirstName();
        public abstract void setFirstName(String firstName);

        @JField(storageId = 104)
        public abstract float getAge();
        public abstract void setAge(float age);

        @OnVersionChange(oldVersion = 2, newVersion = 3)
        private void versionChange(Map<Integer, Object> oldValues) {
            this.setAge((float)(int)((Integer)oldValues.get(103)));
        }
    }

// Version 4

    @JSimpleClass(storageId = 100)
    public abstract static class Person4 implements JObject, HasName {

        @JField(storageId = 105)
        @NotNull
        public abstract Name getName();
        public abstract void setName(Name name);

        @JField(storageId = 104)
        public abstract float getAge();
        public abstract void setAge(float age);

        @OnVersionChange(newVersion = 4)
        private void versionChange(int oldVersion, Map<Integer, Object> oldValues) {
            switch (oldVersion) {
            case 2:
            case 3:
                final Name name = JTransaction.getCurrent().create(Name.class);
                name.setLastName((String)oldValues.get(101));
                name.setFirstName((String)oldValues.get(102));
                this.setName(name);
                break;
            default:
                break;
            }
        }
    }

    @JSimpleClass(storageId = 200)
    public abstract static class Name implements JObject, HasName {

        @JField(storageId = 201, indexed = true)
        @Override
        public abstract String getLastName();
        public abstract void setLastName(String lastName);

        @JField(storageId = 202)
        @Override
        public abstract String getFirstName();
        public abstract void setFirstName(String firstName);
    }
}

