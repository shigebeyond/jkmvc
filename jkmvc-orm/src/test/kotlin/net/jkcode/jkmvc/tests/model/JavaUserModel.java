package net.jkcode.jkmvc.tests.model;

import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KClass;
import net.jkcode.jkmvc.orm.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    @Nullable
    public final String getAvatar() {
        return get("avatar");
    }

    public final void setAvatar(String value) {
        set("avatar", value);
    }

    @Nullable
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
}