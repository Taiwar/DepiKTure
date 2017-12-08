package net.temporus.depikture.utils

import android.content.Context
import android.content.SharedPreferences
import android.support.annotation.BoolRes
import android.support.annotation.IntegerRes
import android.support.annotation.StringRes
import org.jetbrains.anko.defaultSharedPreferences

object Prefs {

    private var mPrefs: SharedPreferences? = null

    fun with(context: Context): SharedPreferences {
        if (mPrefs == null) {
            val appContext = context.applicationContext
            if (appContext != null) {
                mPrefs = appContext.defaultSharedPreferences
            } else {
                throw IllegalArgumentException("context.getApplicationContext returned null")
            }
        }
        return mPrefs!!
    }

}

fun SharedPreferences.getBoolean(ctx: Context, @StringRes prefKeyRes: Int, @BoolRes defValueRes: Int)
        : Boolean = getBoolean(ctx.getString(prefKeyRes), ctx.resources.getBoolean(defValueRes))

fun SharedPreferences.getInt(ctx: Context, @StringRes prefKeyRes: Int, @IntegerRes defValueRes: Int)
        : Int = getInt(ctx.getString(prefKeyRes), ctx.resources.getInteger(defValueRes))

fun SharedPreferences.getStringAsInt(ctx: Context, @StringRes prefKeyRes: Int, @IntegerRes defValueRes: Int)
        : Int = getString(ctx.getString(prefKeyRes), ctx.resources.getInteger(defValueRes).toString()).toInt()

fun SharedPreferences.getStringAsString(ctx: Context, @StringRes prefKeyRes: Int, @StringRes defValueRes: Int)
        : String = getString(ctx.getString(prefKeyRes), ctx.resources.getString(defValueRes))