package peakchao.com.porn.model

import java.io.Serializable

data class PornModel(
    var id: Long? = null,
    var viewKey: String? = null,
    var title: String? = null,
    var imgUrl: String? = null,
    var duration: String? = null,
    var info: String? = null
) : Serializable
