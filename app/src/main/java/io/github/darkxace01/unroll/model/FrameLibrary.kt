package io.github.darkxace01.unroll.model

/** A ready-made photo frame: a [kind] in a colourway. [mount] null = the theme's paper colour. */
data class FramePreset(val id: String, val name: String, val group: String, val kind: FrameKind, val mount: Long? = null, val radius: Float = 0f) {
    fun style(base: PhotoStyle = PhotoStyle()) = base.copy(
        frame = kind,
        mountRole = if (mount == null) ColorRole.Paper else null,
        mountColor = mount ?: 0xFFFFFFFF,
        cornerRadius = radius,
        shadow = if (kind == FrameKind.None && radius == 0f) base.shadow else maxOf(base.shadow, 12f),
    )
}

/** Every frame in the picker, grouped like a shop shelf. */
object FrameLibrary {
    val groups = listOf("Instant", "Film", "Paper", "Devices", "Digital", "Shapes", "Lace & scribbles")

    private val instantColours = listOf(null to "Theme", 0xFFFFFFFFL to "White", 0xFFF6F0E1L to "Cream", 0xFF1D1D1FL to "Black",
        0xFFF6CFD8L to "Pink", 0xFFCFE3F3L to "Sky", 0xFFCDEBDDL to "Mint", 0xFFF8E7A6L to "Butter")

    val all: List<FramePreset> by lazy {
        buildList {
            for (k in listOf(FrameKind.Polaroid, FrameKind.Instax, FrameKind.Square, FrameKind.Wide)) for ((c, n) in instantColours)
                add(FramePreset("${k.name}-$n".lowercase(), "${k.label}, ${n.lowercase()}", "Instant", k, c))
            add(FramePreset("film", "Film strip", "Film", FrameKind.Film))
            add(FramePreset("film-brown", "Film strip, brown", "Film", FrameKind.Film, 0xFF3A2A20))
            add(FramePreset("negative", "Negative", "Film", FrameKind.Negative))
            add(FramePreset("negative-faded", "Negative, faded", "Film", FrameKind.Negative, 0xFFC98848))
            for ((c, n) in listOf(0xFFFFFFFFL to "white", 0xFFF1E9D6L to "cream", 0xFF1D1D1FL to "black", 0xFF9A9AA0L to "grey", 0xFFC8473CL to "red"))
                add(FramePreset("slide-$n", "Slide mount, $n", "Film", FrameKind.Slide, c))
            for ((k, cs) in listOf(
                FrameKind.Stamp to listOf(0xFFFFFCF4L to "white", 0xFFF1E3C6L to "cream", 0xFFD9B98EL to "kraft"),
                FrameKind.Deckle to listOf(0xFFFFFFFFL to "white", 0xFFF3EBDCL to "cream", 0xFF1D1D1FL to "black"),
                FrameKind.Postcard to listOf(0xFFFFFFFFL to "white", 0xFFF3EBDCL to "cream"),
                FrameKind.Corners to listOf(0xFFFFFFFFL to "white", 0xFFF3EBDCL to "cream", 0xFF2A2A2CL to "black"),
                FrameKind.Notebook to listOf(0xFFFFFFFFL to "white", 0xFFFBF5E6L to "cream"),
            )) for ((c, n) in cs) add(FramePreset("${k.name}-$n".lowercase(), "${k.label}, $n", "Paper", k, c))
            add(FramePreset("taped", "Taped", "Paper", FrameKind.Taped))
            add(FramePreset("torn", "Torn paper", "Paper", FrameKind.Torn))
            for ((c, n) in listOf(null to "black", 0xFFD9DADCL to "silver", 0xFFE9D2B3L to "gold", 0xFFF0C9CFL to "pink", 0xFF9DB7D6L to "blue", 0xFFF4F4F2L to "white"))
                add(FramePreset("phone-$n", "Phone, $n", "Devices", FrameKind.Phone, c))
            for ((c, n) in listOf(null to "black", 0xFFCFD1D4L to "silver", 0xFFE9E1CFL to "retro cream", 0xFFB33A33L to "red"))
                add(FramePreset("camera-$n", "Camera, $n", "Devices", FrameKind.Camera, c))
            for ((k, cs) in listOf(
                FrameKind.Browser to listOf(0xFFFFFFFFL to "light", 0xFF2A2B2FL to "dark", null to "theme"),
                FrameKind.Player to listOf(0xFFFFFFFFL to "light", 0xFF1F1F23L to "dark", null to "theme", 0xFFF6D3DCL to "pink"),
                FrameKind.Message to listOf(null to "blue", 0xFF34C759L to "green", 0xFFE5E5EAL to "grey"),
                FrameKind.Popup to listOf(0xFFFFFFFFL to "light", 0xFF2A2B2FL to "dark"),
            )) for ((c, n) in cs) add(FramePreset("${k.name}-$n".lowercase().replace(' ', '-'), "${k.label}, $n", "Digital", k, c))
            add(FramePreset("plain", "Plain", "Shapes", FrameKind.None))
            add(FramePreset("rounded", "Rounded", "Shapes", FrameKind.None, radius = 48f))
            for (k in listOf(FrameKind.Circle, FrameKind.Arch, FrameKind.Oval, FrameKind.Heart, FrameKind.Blob))
                add(FramePreset(k.name.lowercase(), k.label, "Shapes", k))
            for ((c, n) in listOf(0xFFF4EFE6L to "white", 0xFF5E4634L to "wood", 0xFF2F5A45L to "green"))
                add(FramePreset("window-$n", "Window, $n", "Shapes", FrameKind.Window, c))
            for ((c, n) in listOf(0xFFF4ECD8L to "cream", 0xFFFFFFFFL to "white", 0xFFF6D3DCL to "pink"))
                add(FramePreset("ticket-$n", "Ticket, $n", "Shapes", FrameKind.Ticket, c))
            for ((c, n) in listOf(0xFFFFFFFFL to "white", 0xFFF3E9D2L to "ivory", 0xFFF6D3DCL to "pink", 0xFF1D1D1FL to "black"))
                add(FramePreset("lace-$n", "Lace, $n", "Lace & scribbles", FrameKind.Lace, c))
            add(FramePreset("scribble", "Scribble", "Lace & scribbles", FrameKind.Scribble))
        }
    }

    fun byId(id: String) = all.firstOrNull { it.id == id }
}
