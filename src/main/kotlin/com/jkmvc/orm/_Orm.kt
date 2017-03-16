package com.jkmvc.orm

public fun Orm?.isLoaded(): Boolean {
    return this != null && this.loaded;
}