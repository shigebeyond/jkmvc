package net.jkcode.jkmvc.tests;

import net.jkcode.jkmvc.tests.model.JavaUserModel;

public class JavaOrmTests {

    public static void main(String[] args){
        JavaUserModel user = JavaUserModel.ormMeta.queryBuilder(false, false, true)
                .with("home")
                .with("addresses")
                .where("name", "shi")
                .findModel(JavaUserModel.class);
        System.out.println(user);
        System.out.println(user.getHome());
        System.out.println(user.getAddresses());
    }
}
