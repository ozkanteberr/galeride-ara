package com.galeriara.data.encoding

import com.galeriara.core.ModelIdentity

/** Pinned A06 runtime manifest. Update all components and the revision together. */
object MultilingualClipProfile {
    val identity = ModelIdentity(
        id = "clip-vit-b32-multilingual-v1",
        revision = "b01d72b7de9160a38730f6a8102bbbcbc799bb5a153caf9df6e21eec302a310d",
        dimensions = 512,
    )
    const val TEXT_SHA256 = "fbc8fbeaa5237d96bd1bf430057c70d34de8334a463e933a5faa305f6caeed9c"
    const val DENSE_SHA256 = "e5f6548cbcef6c62631b3946d89042ab4c8451762c7bb0a3be11eb6c480b686d"
    const val VOCAB_SHA256 = "fe0fda7c425b48c516fc8f160d594c8022a0808447475c1a7c6d6479763f310c"
    const val OUTPUT_NAME = "last_hidden_state"
}
