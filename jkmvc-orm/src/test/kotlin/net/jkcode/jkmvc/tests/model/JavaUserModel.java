package net.jkcode.jkmvc.tests.model;

import com.google.common.collect.Maps;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KClass;
import net.jkcode.jkmvc.orm.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import kotlin.jvm.functions.Function1;

public final class JavaUserModel extends Orm
{
    public final Integer getId() {
        return get("id");
    }

    public final void setId(Integer value) {
        set("id", value);
    }

    @NotNull
    public final String getName() {
        return get("name");
    }

    public final void setName(String value) {
        set("name", value);
    }

    public final int getAge() {
        return get("age");
    }

    public final void setAge(int value) {
        set("age", value);
    }

    public final String getAvatar() {
        return get("avatar");
    }

    public final void setAvatar(String value) {
        set("avatar", value);
    }

    public final AddressModel getHome() {
        return get("home");
    }

    public final void setHome(AddressModel value) {
        set("home", value);
    }

    @NotNull
    public final List<AddressModel> getAddresses() {
        return get("addresses");
    }

    public final void setAddresses(List<AddressModel> value) {
        set("addresses", value);
    }

    public void beforeCreate() {
        System.out.println("处理 beforeCreate 事件");
    }

    public void beforeUpdate() {
        System.out.println("处理 beforeUpdate 事件");
    }

    public void afterUpdate() {
        System.out.println("处理 afterUpdate 事件");
    }

    public void afterSave() {
        System.out.println("处理 afterSave 事件");
    }

    public void beforeDelete() {
        System.out.println("处理 beforeDelete 事件");
    }

    public void afterDelete() {
        System.out.println("处理 afterDelete 事件");
    }

    /**
     * java orm类的元数据是名为`ormMeta`的属性
     */
    public static final OrmMeta ormMeta = new OrmMeta(JavaUserModel.class, "user", "user");
    static{
        ormMeta.addRule("name", "姓名", "notEmpty", null);
        ormMeta.addRule("age", "年龄", "between(1,120)", null);
        ormMeta.hasOne("home", AddressModel.class, "user_id", "id", Collections.emptyMap(), false,
                new Function2<OrmQueryBuilder, Boolean, Unit>(){

                    @Override
                    public Unit invoke(OrmQueryBuilder query, Boolean lazy) {
                        if(lazy)
                            query.where("is_home", 1);
                        else
                            query.on("is_home", 1, false);
                        return null;
                    }
                });
        ormMeta.hasMany("addresses", AddressModel.class);
    }
}