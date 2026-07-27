package com.iconchanger.wallpaper.rolling.icons.utils

import com.cscmobi.libraryads.commons.remote.CSCKonfigModel
import com.cscmobi.libraryads.commons.remote.konfig

object RemoteConfigs : CSCKonfigModel {

    val native_all by konfig("native_all", true)
    val native_home by konfig("native_home", true)
    val banner_collap_home by konfig("banner_collap_home", true)
    val inter_click by konfig("inter_click", true)


}