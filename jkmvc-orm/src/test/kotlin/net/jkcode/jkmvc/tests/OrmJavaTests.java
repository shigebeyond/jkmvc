package net.jkcode.jkmvc.tests;

import net.jkcode.jkmvc.tests.model.UserModel;

public class OrmJavaTests {

    public static void main(String[] args){
        UserModel user = UserModel.m.queryBuilder(false, false, true)
                .with("home")
                .with("addresses")
                .where("name", "shi")
                .findModel(UserModel.class);
        System.out.println(user);
        System.out.println(user.getHome());
        System.out.println(user.getAddresses());
    }
}
