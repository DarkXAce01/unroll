package io.github.darkxace01.unroll.model

import kotlinx.serialization.Serializable

/**
 * Colours and fonts by job, not by value. Template items say "I'm Ink" or "I'm the headline font", so
 * switching the theme restyles a whole carousel in one tap. Picking an exact colour on an item drops its
 * role and it stays that colour.
 */
@Serializable
enum class ColorRole(val label: String) { Background("Background"), Paper("Paper"), Ink("Ink"), Accent("Accent"), Muted("Muted") }

@Serializable
enum class FontRole { Head, Body, Hand }

@Serializable
data class Theme(
    val name: String,
    val background: Long,
    val paper: Long,
    val ink: Long,
    val accent: Long,
    val muted: Long,
    val head: FontKey,
    val body: FontKey,
    val hand: FontKey = FontKey.Hand,
) {
    fun color(r: ColorRole): Long = when (r) {
        ColorRole.Background -> background
        ColorRole.Paper -> paper
        ColorRole.Ink -> ink
        ColorRole.Accent -> accent
        ColorRole.Muted -> muted
    }

    fun font(r: FontRole): FontKey = when (r) {
        FontRole.Head -> head
        FontRole.Body -> body
        FontRole.Hand -> hand
    }
}

object Themes {
    val Paper = Theme("Paper", 0xFFF4F1EA, 0xFFFFFFFF, 0xFF1D1E22, 0xFFD4432A, 0xFF8A857C, FontKey.Elegant, FontKey.Sans)
    val Midnight = Theme("Midnight", 0xFF14172B, 0xFF22264A, 0xFFF2EFE8, 0xFFF2B33D, 0xFF8C90B0, FontKey.Display, FontKey.Sans)
    val Sand = Theme("Sand", 0xFFE9DCC6, 0xFFF7F0E4, 0xFF4A3426, 0xFFC0623B, 0xFF9C8670, FontKey.ElegantItalic, FontKey.Serif)
    val Ocean = Theme("Ocean", 0xFFDDEBEE, 0xFFFFFFFF, 0xFF0F3341, 0xFF2A8FA8, 0xFF6F8F98, FontKey.Grotesk, FontKey.Sans)
    val Forest = Theme("Forest", 0xFF1F3A2E, 0xFFEDE8DA, 0xFFF1ECDD, 0xFFE0A33A, 0xFF8DA696, FontKey.Elegant, FontKey.Condensed)
    val Blush = Theme("Blush", 0xFFF6E1E1, 0xFFFFFFFF, 0xFF5A2E36, 0xFFE0607E, 0xFFB08A90, FontKey.ElegantItalic, FontKey.Grotesk)
    val Sunset = Theme("Sunset", 0xFFF7D9B8, 0xFFFFF6EC, 0xFF3B1F2B, 0xFFE8603C, 0xFFA87C6A, FontKey.Display, FontKey.Grotesk)
    val Mono = Theme("Mono", 0xFFFFFFFF, 0xFFF0F0F0, 0xFF111111, 0xFF111111, 0xFF888888, FontKey.Condensed, FontKey.Mono)
    val Retro = Theme("Retro", 0xFFF3E7C9, 0xFFFFF8E6, 0xFF2B2A5C, 0xFFE4572E, 0xFF76A08A, FontKey.Display, FontKey.Pixel)
    val Lilac = Theme("Lilac", 0xFFE7E1F4, 0xFFFFFFFF, 0xFF2E2547, 0xFF7D5BD6, 0xFF9C94B3, FontKey.Grotesk, FontKey.Sans)
    val Noir = Theme("Noir", 0xFF0E0E10, 0xFF1C1C20, 0xFFF5F5F5, 0xFFE33B3B, 0xFF7A7A80, FontKey.Condensed, FontKey.Sans)
    val Citrus = Theme("Citrus", 0xFFFFF4C2, 0xFFFFFFFF, 0xFF1F2A1A, 0xFF5FA036, 0xFF9AA37E, FontKey.Grotesk, FontKey.Grotesk)

    val all = listOf(Paper, Midnight, Sand, Ocean, Forest, Blush, Sunset, Mono, Retro, Lilac, Noir, Citrus)
}

/** How a photo is mounted. The item's frame is the outside; the photo sits in [inner]. */
@Serializable
enum class FrameKind(val label: String) {
    None("None"), Polaroid("Polaroid"), Film("Film"), Stamp("Stamp"), Circle("Circle"), Arch("Arch"), Torn("Torn paper"),
    Instax("Mini instant"), Wide("Wide instant"), Square("Square instant"), Slide("Slide mount"), Negative("Negative"),
    Deckle("Deckled print"), Postcard("Postcard"), Corners("Photo corners"), Taped("Taped"), Notebook("Notebook page"),
    Phone("Phone"), Camera("Camera screen"), Browser("Browser"), Player("Music player"), Message("Message"), Popup("Pop-up"),
    Heart("Heart"), Oval("Oval"), Lace("Lace"), Scribble("Scribble"), Blob("Blob"), Window("Window"), Ticket("Ticket"),
}
