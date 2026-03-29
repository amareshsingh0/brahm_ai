package com.bimoraai.brahm.ui.palmistry

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ─── Data ─────────────────────────────────────────────────────────────────────

private data class HandType(
    val id: String, val label: String, val vedic: String, val shape: String,
    val element: String, val icon: String, val traits: List<String>,
    val careers: String, val constitution: String, val shadow: String, val vedicNote: String,
    val modifier: Map<String, Int>,
)

private val HAND_TYPES = listOf(
    HandType("earth","Earth Hand","Prithvi Hasta","Square palm + Short fingers","Earth (Prithvi)","🌍",
        listOf("Practical & reliable","Hardworking","Strong physical constitution","Prefers routine","Hands-on learner"),
        "Builder, surgeon, chef, craftsman, athlete, engineer",
        "Kapha (Ayurveda) — stable, grounding, enduring",
        "Stubbornness, resistance to change, overattachment to the material",
        "Bhu-Tattva dominant. Deeply connected to the material plane. The karmic path involves mastery through physical effort and grounded wisdom.",
        mapOf("health" to 2,"wealth" to 1,"love" to 0,"mental" to -1,"spiritual" to -1,"career" to 2)),
    HandType("air","Air Hand","Vayu Hasta","Square palm + Long fingers","Air (Vayu)","💨",
        listOf("Intellectual","Curious & versatile","Excellent communicator","Social","Ideas-driven"),
        "Writer, journalist, teacher, scientist, lawyer, philosopher",
        "Vata (Ayurveda) — swift, mercurial, adaptive",
        "Anxiety, restlessness, difficulty grounding ideas into reality",
        "Vayu-Tattva dominant. Ruled by Budha (Mercury). The karmic path involves using the gift of communication to serve truth and uplift others.",
        mapOf("health" to -1,"wealth" to 1,"love" to 1,"mental" to 2,"spiritual" to 1,"career" to 1)),
    HandType("water","Water Hand","Jala Hasta","Long oval palm + Long fine fingers","Water (Jala)","💧",
        listOf("Deeply intuitive","Empathetic","Creative & artistic","Psychically sensitive","Absorbs others' energies"),
        "Healer, astrologer, artist, psychologist, poet, musician",
        "Kapha-Pitta (Ayurveda) — fluid, receptive, emotionally rich",
        "Over-sensitivity, poor boundaries, escapism, emotional overwhelm",
        "Jala-Tattva dominant. Ruled by Chandra (Moon). The karmic path involves learning to channel deep sensitivity into healing and creative gifts without losing self.",
        mapOf("health" to 0,"wealth" to -1,"love" to 2,"mental" to 1,"spiritual" to 2,"career" to 0)),
    HandType("fire","Fire Hand","Agni Hasta","Long palm + Short fingers","Fire (Agni)","🔥",
        listOf("Passionate & energetic","Natural leader","Charismatic & inspiring","Ambitious","Impatient"),
        "Entrepreneur, military leader, performer, athlete, politician, pioneer",
        "Pitta (Ayurveda) — radiant, forceful, transforming",
        "Arrogance, burnout, aggression, inability to rest or surrender",
        "Agni-Tattva dominant. Ruled by Surya + Mangal. The karmic path involves tempering the fire of ambition with humility and compassion, so the flame uplifts rather than burns.",
        mapOf("health" to 1,"wealth" to 2,"love" to 0,"mental" to 0,"spiritual" to -1,"career" to 2)),
)

private data class PalmOption(val id: String, val label: String, val desc: String, val meaning: String, val vedic: String, val scores: Map<String, Int>)
private data class PalmQuestion(val id: String, val line: String, val sanskrit: String, val instruction: String, val hint: String, val color: Color, val options: List<PalmOption>)

private val QUESTIONS = listOf(
    PalmQuestion("heart","Heart Line","हृदय रेखा","Look at the TOPMOST horizontal line running across your upper palm.","It runs from below your little finger toward your index or middle finger.",Color(0xFFE8650A),listOf(
        PalmOption("long_curved","Long & Deeply Curved","Sweeps up toward the index/middle finger in a strong arc","Warm, expressive, deeply romantic. You love unconditionally and wear your heart on your sleeve. Relationships are central to your life purpose.","Strong Shukra (Venus) influence — blessings in love, partnership, and devotion. The heart is your greatest teacher.",mapOf("love" to 3,"spiritual" to 1,"health" to 1)),
        PalmOption("straight_short","Straight & Short","Relatively straight, doesn't reach as far across the palm","Practical and measured in emotion. You love deeply but express it through loyal actions rather than grand gestures. Reliability is your love language.","Balanced Chandra (Moon) — emotional intelligence without overwhelm. Disciplined heart.",mapOf("love" to 1,"mental" to 1,"wealth" to 1,"career" to 1)),
        PalmOption("forked","Forked at End","The line splits into two branches near the end","Rare and auspicious. You balance heart and mind beautifully — empathetic yet rational. Natural counselor or mediator.","Budha-Chandra yoga — harmonious mind-heart communication. The fork indicates Viveka (discernment).",mapOf("love" to 2,"spiritual" to 1,"mental" to 2,"career" to 1)),
        PalmOption("high_index","Ends Below Index Finger","The line curves up and ends near the base of your index finger","Idealistic in love. You seek nothing less than a divine, soulmate-level connection. High standards — and you deserve them.","Guru (Jupiter) sub-influence on the heart — seeks Prema (divine love). Marriage as spiritual sadhana.",mapOf("love" to 2,"spiritual" to 2)),
        PalmOption("broken","Broken or Chained","Has gaps, islands, or chain-like appearance","Significant emotional journey. Past heartbreaks have shaped you deeply. These scars are also sources of immense compassion and wisdom.","Rahu/Ketu axis — karmic relationship lessons carried across lifetimes. The break is a portal to transformation.",mapOf("spiritual" to 2,"mental" to -1,"health" to -1)),
    )),
    PalmQuestion("head","Head Line","मस्तिष्क रेखा","Find the MIDDLE horizontal line running across your palm.","It starts near the edge between your thumb and index finger, running across toward the outer palm.",Color(0xFFC8860A),listOf(
        PalmOption("long_straight","Long & Straight","Extends nearly across the full width of the palm","Razor-sharp analytical mind. You excel at logic, mathematics, science, law, and strategic thinking. You see patterns others miss.","Strong Budha (Mercury) — exceptional intellectual capacity. Saraswati's grace on the mind.",mapOf("mental" to 3,"career" to 2,"wealth" to 1,"love" to -1)),
        PalmOption("curved_down","Deeply Curved Downward","Curves noticeably downward toward the base of the palm","Highly creative and imaginative. You think in images, metaphors, and stories. Writer, artist, visionary.","Chandra dominance on Buddhi — imaginative intelligence. Saraswati as Vak Shakti.",mapOf("mental" to 2,"career" to 1,"love" to 1,"spiritual" to 2,"wealth" to -1)),
        PalmOption("fork_end","Forked at End (Writer's Fork)","Splits into two branches near the end","The most auspicious head line. You hold both analytical and creative intelligence simultaneously.","Saraswati blessing — dual vision, the gift of eloquence.",mapOf("mental" to 3,"career" to 2,"love" to 1,"spiritual" to 1,"wealth" to 1)),
        PalmOption("short","Short","Doesn't extend very far across the palm","Decisive and action-oriented. You trust your instincts over lengthy analysis. Quick thinking, entrepreneurial spirit.","Mangal (Mars) influence on intellect — courage of thought. The instinct IS the intelligence.",mapOf("mental" to 1,"career" to 2,"health" to 1,"wealth" to 1)),
        PalmOption("chained","Wavy or Chained","Appears uneven, wavy, or has chain-like links along it","Sensitive, creative, and deeply perceptive. Your mind picks up subtle energies others miss.","Ketu influence on Manas — a mind that perceives across dimensions. Highly psychic but needs grounding.",mapOf("mental" to 1,"love" to 1,"spiritual" to 3,"health" to -1,"wealth" to -1)),
    )),
    PalmQuestion("life","Life Line","जीवन रेखा","Find the curved line that sweeps around the base of your thumb.","It starts between your index finger and thumb, curving down around the thumb mount (Venus mount).",Color(0xFF6D28D9),listOf(
        PalmOption("wide_deep","Wide Arc & Deep","Makes a large, sweeping arc around the thumb","Abundant Prana (life force). You have robust vitality, strong physical resilience, and a genuine love of life.","Surya-Mangal blessings — solar vitality and warrior constitution. Strong Ojas (immunity).",mapOf("health" to 3,"career" to 2,"love" to 1,"wealth" to 1)),
        PalmOption("long_clear","Long & Unbroken","Extends far down the palm with no breaks or islands","Steady, sustained vitality throughout life. Few major health crises. Your energy is reliable and consistent.","Dhanvantari grace — blessed health karma from prior lifetimes of righteous living.",mapOf("health" to 3,"career" to 1,"mental" to 1)),
        PalmOption("double","Double Life Line","A parallel line runs alongside the main life line","Extraordinary — this is the Kavach Rekha (protective mark). You carry divine protection.","Pitru Kavach and Ishta Devata's active blessing. One of the most auspicious marks in Samudrika Shastra.",mapOf("health" to 3,"career" to 1,"love" to 1,"mental" to 1,"spiritual" to 3,"wealth" to 1)),
        PalmOption("close_thumb","Close to Thumb (Tight Arc)","Stays close to the thumb, doesn't sweep far out","More introverted energy. You conserve and channel your vitality with precision. Quality over quantity in all things.","Shani influence on Prana — teaches conservation and mastery. The monk archetype.",mapOf("health" to 1,"mental" to 2,"spiritual" to 2,"love" to -1)),
        PalmOption("broken","Broken or Has Islands","Has visible gaps, breaks, or island shapes along it","Life has involved major transformations. A break is not an ending — it is a rebirth. You are a survivor and a transformer.","Rahu transit marker — radical life pivots that serve divine redirection. The phoenix pattern.",mapOf("health" to -1,"career" to -1,"spiritual" to 2,"wealth" to -1)),
    )),
    PalmQuestion("fate","Fate Line","भाग्य रेखा","Look for a vertical line running up the CENTER of your palm.","It runs from the base of your palm upward toward your middle (Saturn) finger. Some palms don't have this line.",Color(0xFFC8860A),listOf(
        PalmOption("deep_clear","Deep & Clear from Wrist","A strong, clear vertical line running from near the wrist upward","Powerful dharmic purpose. Your life path is clear and purposeful from an early age. Career feels like a calling.","Shani yoga strong — disciplined effort is magnificently rewarded. Prarabdha karma is guiding you firmly.",mapOf("career" to 3,"wealth" to 2,"mental" to 1,"spiritual" to 1)),
        PalmOption("starts_late","Starts in Middle of Palm","The fate line only begins halfway up the palm","A late bloomer — and that's your superpower. Life clarity and career success come after age 30–35.","Shani dasha activation — Saturn's gifts come after his tests. The deepest roots take longest to grow.",mapOf("career" to 2,"wealth" to 1,"mental" to 1,"spiritual" to 1)),
        PalmOption("broken_shifting","Broken or Shifts Direction","Has breaks, or noticeably changes direction midway","Multiple life chapters and career pivots. Each phase is complete in itself. You're not inconsistent — you're evolving.","Rahu-Shani interaction — disruption precedes breakthrough. The snake sheds its skin.",mapOf("career" to 1,"love" to 1,"spiritual" to 2)),
        PalmOption("absent","Not Visible / Absent","No clear vertical line in the center of the palm","Complete free will. You are not bound by a predetermined destiny — you are the author of your own story.","Moksha marker — free from karma's direct grip. Pure agency. A life of Kriyamana karma.",mapOf("career" to 1,"wealth" to 1,"spiritual" to 2)),
        PalmOption("double","Double Fate Line","Two parallel vertical lines running up the palm","Extraordinarily rare and auspicious. You are gifted with dual dharma — two parallel callings.","Double Shani Rekha — blessed with dual purpose.",mapOf("career" to 3,"wealth" to 2,"mental" to 1,"love" to 1,"spiritual" to 1)),
    )),
    PalmQuestion("sun","Sun Line","सूर्य रेखा","Look for a vertical line near your RING finger.","It runs upward toward your ring (Apollo) finger, parallel to the fate line.",Color(0xFFE8650A),listOf(
        PalmOption("strong_clear","Strong & Clear","A noticeable vertical line clearly approaching the ring finger","Fame, recognition, and creative success are yours. You have a natural public presence — people notice you.","Surya strong — solar radiance, fame as the result of dharmic action in past lives.",mapOf("career" to 2,"wealth" to 1,"love" to 1,"spiritual" to 1)),
        PalmOption("multiple_short","Multiple Short Lines","Several small lines near the ring finger","Many talents and creative gifts — but scattered. Focusing your energy on one core creative path will unlock recognition.","Surya dispersed — needs a single dharmic focus to crystallize into fame.",mapOf("career" to 1,"mental" to 1,"spiritual" to 1)),
        PalmOption("starts_heart","Starts from Heart Line","The sun line appears to begin from where the heart line is","Recognition comes through authentic passion and love. Success arrives in mid-to-late life — permanent and deeply fulfilling.","Delayed Surya grace — built on devotion rather than ambition.",mapOf("career" to 2,"wealth" to 1,"love" to 2,"spiritual" to 2)),
        PalmOption("absent","Absent / Not Visible","No clear line near the ring finger","Success without public fame. You achieve significantly — but prefer to work behind the scenes.","Inner Surya — the light shines inward rather than outward. Spiritual achievement over worldly recognition.",mapOf("mental" to 1,"spiritual" to 2)),
    )),
    PalmQuestion("mount_venus","Mount of Venus","शुक्र पर्वत","Look at the fleshy area at the BASE of your THUMB.","Press your thumb gently to your palm — the padded area that rises is the Venus mount.",Color(0xFFE8650A),listOf(
        PalmOption("large_full","Large & Well Developed","Full, firm, and prominent — noticeably raised","Abundant capacity for love, pleasure, beauty, and sensuality. You give generously in relationships. Naturally creative.","Shukra strong — Kama (desire) is healthy and abundant. Great blessings in love and artistic pursuits.",mapOf("love" to 3,"health" to 1)),
        PalmOption("medium","Medium / Moderate","Present but not especially prominent or flat","Balanced approach to love and pleasure. You enjoy life's sensory gifts without being controlled by them.","Shukra in balance — the middle path of Kama. Healthy desire without attachment.",mapOf("love" to 1,"spiritual" to 1,"health" to 1,"wealth" to 1)),
        PalmOption("flat","Flat or Barely Visible","The base of the thumb area is relatively flat","More reserved in emotional and sensory expression. Relationships may feel secondary to work or spiritual pursuits.","Shukra subdued — Vairagya (detachment) over Bhoga (enjoyment). The renunciant archetype.",mapOf("spiritual" to 2,"mental" to 1,"career" to 1,"love" to -1)),
    )),
    PalmQuestion("mount_jupiter","Mount of Jupiter","गुरु पर्वत","Look at the base of your INDEX finger.","The padded area directly below the index finger — when developed, it creates a visible fullness.",Color(0xFFC8860A),listOf(
        PalmOption("prominent","Prominent & Raised","Noticeably full area below the index finger","Natural leader, teacher, and guide. You have strong ambition, confidence, and a genuine desire to uplift others.","Guru strong — Brahma Jnana, leadership blessed by Jupiter. The Guru archetype.",mapOf("career" to 3,"love" to 1,"mental" to 1,"wealth" to 2,"spiritual" to 2)),
        PalmOption("moderate","Moderate","Present but not dramatically raised","Healthy confidence and ambition. You lead when needed but don't seek the spotlight for its own sake.","Guru in balance — wisdom applied practically. Dharmic leadership.",mapOf("career" to 1,"mental" to 1,"wealth" to 1,"spiritual" to 1)),
        PalmOption("flat","Flat or Underdeveloped","Little or no fullness below the index finger","May struggle with confidence or claiming authority. Leadership may feel uncomfortable. The inner Jupiter is waiting.","Guru dormant — the teacher within is not yet activated. Faith and study will awaken it.",mapOf("career" to -1,"wealth" to -1)),
    )),
)

private data class Mount(val id: String, val name: String, val sanskrit: String, val finger: String, val planet: String, val icon: String, val color: Color, val well: String, val flat: String, val over: String, val vedic: String)
private val MOUNTS = listOf(
    Mount("jupiter","Mount of Jupiter","गुरु पर्वत","Below Index Finger","Jupiter (Guru)","♃","#C8860A".toArgbColor(),"Leader, ambitious, spiritually wise, generous, charismatic. Born to guide and teach.","Lack of confidence, avoids leadership roles.","Arrogant, overbearing, domineering.","Guru's seat of Brahma Jnana. Prominent in teachers, judges, and spiritual leaders."),
    Mount("saturn","Mount of Saturn","शनि पर्वत","Below Middle Finger","Saturn (Shani)","♄","#7A8BAA".toArgbColor(),"Wise, responsible, patient, deeply introspective. Excellent researchers and dedicated servants.","Avoids responsibility, lacks discipline.","Melancholic, hermit tendencies, can't enjoy life.","Shani's seat of karma and dharma. Those who bear great responsibility with grace."),
    Mount("apollo","Mount of Apollo","सूर्य पर्वत","Below Ring Finger","Sun (Surya)","☉","#E8650A".toArgbColor(),"Creative, charismatic, drawn to beauty and art. Fame comes naturally. Joyful and warm-hearted.","Lack of aesthetic sense, avoids public life.","Vanity, obsession with appearances, craving fame over substance.","Surya's seat of Atmic brilliance. Artists, performers, and those with deep creative Shakti."),
    Mount("mercury","Mount of Mercury","बुध पर्वत","Below Little Finger","Mercury (Budha)","☿","#7A8BAA".toArgbColor(),"Brilliant communicator, witty, business-minded, quick-thinking. Natural healer.","Communication difficulties, poor business instinct.","Cunning, deceptive use of words.","Budha's seat of Vak Shakti. Scholars, traders, orators, and diplomats."),
    Mount("venus","Mount of Venus","शुक्र पर्वत","Base of Thumb","Venus (Shukra)","♀","#E8650A".toArgbColor(),"Loving, sensual, generous, artistic. Great capacity for pleasure and deep human connection.","Cold, unaffectionate, may struggle with intimacy.","Excessive sensuality, overindulgence, attachment.","Shukra's seat of Kama (desire) — one of the four Purusharthas. The quality of love is here."),
    Mount("moon","Mount of Moon","चन्द्र पर्वत","Outer Base (opposite thumb)","Moon (Chandra)","☽","#7A8BAA".toArgbColor(),"Deeply intuitive, imaginative, psychic sensitivity, rich inner world. Love of travel and poetry.","Lack of imagination, emotionally rigid.","Overly emotional, difficulty distinguishing real from imagined.","Chandra's seat of Manas and intuition. Astrologers, poets, healers, and psychic seers."),
    Mount("mars","Mounts of Mars","मंगल पर्वत","Upper & Lower Inner Edge","Mars (Mangal)","♂","#E8650A".toArgbColor(),"Physical and moral courage. Resilience under pressure. Strength when it matters most.","Cowardice, gives up easily, avoids confrontation.","Aggressive, hot-tempered, prone to confrontation.","Mangal's Agni Shakti — the fire of righteous action. Warriors, surgeons, athletes."),
)

private fun String.toArgbColor(): Color = try {
    val hex = this.removePrefix("#")
    Color(android.graphics.Color.parseColor("#$hex"))
} catch (_: Exception) { Color.Gray }

// ─── Report ───────────────────────────────────────────────────────────────────

private data class LifeScore(val area: String, val score: Int, val icon: String)
private data class PalmRemedy(val title: String, val detail: String)
private data class Selection(val questionId: String, val optionId: String)
private data class PalmReport(
    val handType: HandType, val selections: List<Selection>,
    val lifeScores: List<LifeScore>, val strengths: List<String>,
    val challenges: List<String>, val remedies: List<PalmRemedy>,
    val summary: String, val auspiciousNote: String,
)

private fun buildReport(handTypeId: String, selections: List<Selection>): PalmReport {
    val handType = HAND_TYPES.find { it.id == handTypeId } ?: HAND_TYPES[0]
    val raw = mutableMapOf("love" to 5,"mental" to 5,"health" to 5,"wealth" to 5,"career" to 5,"spiritual" to 5)
    handType.modifier.forEach { (k, v) -> raw[k] = (raw[k] ?: 0) + v }
    selections.forEach { sel ->
        QUESTIONS.find { it.id == sel.questionId }?.options?.find { it.id == sel.optionId }
            ?.scores?.forEach { (k, v) -> raw[k] = (raw[k] ?: 0) + v }
    }
    fun clamp(v: Int) = v.coerceIn(1, 10)
    val lifeScores = listOf(
        LifeScore("Love & Relationships", clamp(raw["love"] ?: 5), "❤️"),
        LifeScore("Career & Purpose",     clamp(raw["career"] ?: 5), "⭐"),
        LifeScore("Wealth & Prosperity",  clamp(raw["wealth"] ?: 5), "💰"),
        LifeScore("Mental Clarity",       clamp(raw["mental"] ?: 5), "🧠"),
        LifeScore("Health & Vitality",    clamp(raw["health"] ?: 5), "🌿"),
        LifeScore("Spiritual Growth",     clamp(raw["spiritual"] ?: 5), "🕉️"),
    )
    val strengths = mutableListOf("${handType.label} (${handType.vedic}) — ${handType.traits[0]}, ${handType.traits[1]}.")
    val challenges = mutableListOf<String>()
    selections.forEach { sel ->
        val q = QUESTIONS.find { it.id == sel.questionId } ?: return@forEach
        val opt = q.options.find { it.id == sel.optionId } ?: return@forEach
        val sum = opt.scores.values.sum()
        if (sum >= 3) strengths.add("${q.line}: ${opt.meaning.split(".")[0]}.")
        if (sum <= -1) challenges.add("${q.line}: ${opt.meaning.split(".").takeLast(2).joinToString(".")}")
    }
    val remedies = mutableListOf<PalmRemedy>()
    if ((raw["love"]     ?: 5) < 5) remedies.add(PalmRemedy("Shukra Mantra",      "Chant 'Om Shukraya Namah' 108 times on Fridays. Wear white and offer white flowers. This strengthens Venus energy and opens the heart."))
    if ((raw["career"]   ?: 5) < 5) remedies.add(PalmRemedy("Shani Stotra",       "Recite the Shani Chalisa on Saturdays. Donate black sesame seeds (til) on Saturdays. This activates Shani's blessings for karma and career."))
    if ((raw["health"]   ?: 5) < 5) remedies.add(PalmRemedy("Surya Namaskar",     "Practice 12 rounds of Surya Namaskar at sunrise daily. Offer water to the rising sun (Arghya). This strengthens Prana and solar vitality."))
    if ((raw["mental"]   ?: 5) < 5) remedies.add(PalmRemedy("Saraswati Vandana",  "Chant the Saraswati Vandana daily before study or creative work. This strengthens Buddhi and Vak Shakti."))
    if ((raw["spiritual"]?: 5) < 5) remedies.add(PalmRemedy("Chandra Meditation", "Meditate facing the moon on Purnima (full moon) nights. Chant 'Om Chandraya Namah' 108 times."))
    if ((raw["wealth"]   ?: 5) < 5) remedies.add(PalmRemedy("Lakshmi Puja",       "Light a ghee lamp on Fridays and recite Sri Sukta. Keep your wallet and home clean and organized."))
    if (remedies.isEmpty()) {
        remedies.add(PalmRemedy("Daily Pranayama", "Practice Anulom-Vilom (alternate nostril breathing) for 10 minutes daily at dawn. This balances Ida and Pingala nadis."))
        remedies.add(PalmRemedy("Mantra Japa",     "Select your Ishta Devata and chant their beej mantra 108 times daily. Consistency over intensity transforms the subconscious over 40 days."))
    }
    if (remedies.size < 3) remedies.add(PalmRemedy("Jyotish Consultation", "For complete guidance, consult a Vedic astrologer who combines palm reading with your Kundali chart. The palm shows the karma; the chart shows the timing."))
    val topAreas = lifeScores.filter { it.score >= 7 }.map { it.area.lowercase() }
    val summary = "Your ${handType.label} (${handType.vedic}) reveals a soul deeply aligned with the ${handType.element} element — ${handType.traits.take(3).joinToString(", ")}. ${handType.vedicNote} The patterns in your palm speak of a life rich with ${topAreas.joinToString(" and ")}. ${if (challenges.isNotEmpty()) "The Vedic tradition teaches that challenges are not obstacles but gurukul — the school of the soul. Your karmic work involves ${challenges[0].split(":")[0].lowercase()} as a primary area of growth." else "The clarity and balance in your palm indicate a soul that has done significant work across lifetimes."} Trust the dharmic path already written in your hands — and walk it with devotion."
    val auspiciousNote = when (handType.id) {
        "fire"  -> "The fire in your hand is divine Agni — the same sacred flame that carries offerings to the gods. Your ambition, when channeled through dharma, becomes worship."
        "water" -> "The depth of your waters mirrors the depth of your soul. Like the ocean that remains calm at its floor even in storms, your inner stillness is your greatest treasure."
        "air"   -> "The winds of Vayu carry Prana — the breath of life. Your mind, like the wind, touches everything and is touched by everything. Your words have the power to heal."
        else    -> "The earth you are made of is the same earth that holds the roots of the Bodhi tree. Your groundedness is sacred — it is the foundation upon which temples are built."
    }
    return PalmReport(handType, selections, lifeScores, strengths, challenges, remedies, summary, auspiciousNote)
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun PalmistryScreen(navController: NavController, vm: PalmistryViewModel = hiltViewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }

    SwipeBackLayout(navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🖐️ Palmistry", fontWeight = FontWeight.Bold, color = BrahmGold) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
                )
            },
            containerColor = BrahmBackground,
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = BrahmBackground, contentColor = BrahmGold, edgePadding = 0.dp) {
                    listOf("🔍 Scan & Read","🖐 Lines","⛰ Mounts","📖 Hand Types").forEachIndexed { i, label ->
                        Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (selectedTab == i) BrahmGold else BrahmMutedForeground,
                                    fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal))
                        }
                    }
                }
                when (selectedTab) {
                    0 -> ScanTab(vm)
                    1 -> LinesTab()
                    2 -> MountsTab()
                    3 -> HandTypeTab()
                }
            }
        }
    }
}

// ─── Scan Tab — Dual Hand Flow ────────────────────────────────────────────────

@Composable
private fun ScanTab(vm: PalmistryViewModel) {
    // Dual-hand state
    val domResult     by vm.domResult.collectAsState()
    val nonDomResult  by vm.nonDomResult.collectAsState()
    val combined      by vm.combined.collectAsState()
    val domLoading    by vm.domLoading.collectAsState()
    val nonDomLoading by vm.nonDomLoading.collectAsState()
    val domError      by vm.domError.collectAsState()
    val nonDomError   by vm.nonDomError.collectAsState()

    // step: choose_hand | scan_dominant | scan_non_dominant | report
    var step          by remember { mutableStateOf("choose_hand") }
    var dominantHand  by remember { mutableStateOf("right") }  // "right" or "left"
    var domUri        by remember { mutableStateOf<Uri?>(null) }
    var nonDomUri     by remember { mutableStateOf<Uri?>(null) }
    var cameraUri     by remember { mutableStateOf<Uri?>(null) }
    var cameraTarget  by remember { mutableStateOf("dom") }     // which hand camera is for

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { if (cameraTarget == "dom") domUri = uri else nonDomUri = uri }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { if (cameraTarget == "dom") domUri = cameraUri else nonDomUri = cameraUri }
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { val uri = vm.createCameraUri(); cameraUri = uri; cameraLauncher.launch(uri) }
    }

    fun launchGallery(target: String) { cameraTarget = target; galleryLauncher.launch("image/*") }
    fun launchCamera(target: String)  { cameraTarget = target; cameraPermLauncher.launch(Manifest.permission.CAMERA) }

    fun reset() {
        step = "choose_hand"; domUri = null; nonDomUri = null
        vm.clearResult()
    }

    // Auto-advance to report when combined result arrives
    LaunchedEffect(combined) { if (combined != null) step = "report" }

    when (step) {
        "choose_hand"      -> ChooseHandStep(dominantHand,
            onSelect   = { dominantHand = it },
            onContinue = { step = "scan_dominant" })

        "scan_dominant"    -> HandScanStep(
            role        = "dominant",
            dominantHand = dominantHand,
            uri         = domUri,
            isLoading   = domLoading,
            error       = domError,
            domResult   = domResult,
            onGallery   = { launchGallery("dom") },
            onCamera    = { launchCamera("dom") },
            onReset     = { domUri = null },
            onAnalyze   = { domUri?.let { vm.analyzeDominant(it) } },
            onNext      = { step = "scan_non_dominant" },
            onBack      = { step = "choose_hand" })

        "scan_non_dominant"-> HandScanStep(
            role        = "non_dominant",
            dominantHand = dominantHand,
            uri         = nonDomUri,
            isLoading   = nonDomLoading,
            error       = nonDomError,
            domResult   = nonDomResult,
            onGallery   = { launchGallery("non_dom") },
            onCamera    = { launchCamera("non_dom") },
            onReset     = { nonDomUri = null },
            onAnalyze   = { nonDomUri?.let { u -> domUri?.let { d -> vm.analyzeNonDominantAndCombine(d, u, dominantHand) } } },
            onNext      = {},
            onBack      = { step = "scan_dominant" })

        "report" -> {
            val c   = combined
            val dom = (c?.get("dominant") as? JsonObject) ?: domResult
            val nd  = (c?.get("non_dominant") as? JsonObject) ?: nonDomResult
            val cmb = c?.get("combined") as? JsonObject
            if (dom != null) DualReportStep(dom, nd, cmb, domUri, nonDomUri, onReset = { reset() })
        }
    }
}

// ─── Step 0: Choose Dominant Hand ─────────────────────────────────────────────

@Composable
private fun ChooseHandStep(dominantHand: String, onSelect: (String) -> Unit, onContinue: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(BrahmBackground), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            // Header
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🖐️", fontSize = 28.sp)
                        Column {
                            Text("Dual Palm Reading", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                            Text("Scan both hands for your complete karmic picture", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    }
                    HorizontalDivider(color = BrahmBorder)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple("dominant",     "✦ Dominant Hand", "Kriyamana Karma\nPresent life & current path"),
                            Triple("non_dominant", "✦ Non-Dominant", "Prarabdha Karma\nPast life & soul origins"),
                        ).forEach { (_, label, desc) ->
                            Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)), border = BorderStroke(1.dp, BrahmGold.copy(alpha = 0.3f))) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmGold))
                                    Text(desc,  style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, lineHeight = 18.sp))
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Which is your dominant hand?", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text("Your dominant hand is the one you write with.", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("right" to "Right Hand", "left" to "Left Hand").forEach { (val_, label) ->
                            val selected = dominantHand == val_
                            Card(
                                onClick = { onSelect(val_) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (selected) BrahmGold.copy(alpha = 0.12f) else Color(0xFFF8F9FA)),
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BrahmGold else BrahmBorder),
                            ) {
                                Column(Modifier.padding(14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(if (val_ == "right") "🤚" else "🖐️", fontSize = 28.sp)
                                    Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = if (selected) BrahmGold else BrahmForeground))
                                }
                            }
                        }
                    }
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BrahmGold)) {
                        Text("Begin Scan →", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Step 1 & 2: Scan a Hand ──────────────────────────────────────────────────

@Composable
private fun HandScanStep(
    role: String,
    dominantHand: String,
    uri: Uri?,
    isLoading: Boolean,
    error: String?,
    domResult: JsonObject?,   // result after analysis (reused for showing "done" state)
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onReset: () -> Unit,
    onAnalyze: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val isDom     = role == "dominant"
    val accentCol = if (isDom) BrahmGold else Color(0xFF7C3AED)
    val handName  = if (isDom) {
        if (dominantHand == "right") "Right Hand (Dominant)" else "Left Hand (Dominant)"
    } else {
        if (dominantHand == "right") "Left Hand (Non-Dominant)" else "Right Hand (Non-Dominant)"
    }
    val karmaLabel = if (isDom) "Kriyamana Karma — Present Life" else "Prarabdha Karma — Past Life & Soul Origins"
    val handEmoji  = if (isDom) "✦" else "☽"
    val instruction = if (isDom)
        "Hold your $handName flat under good light. Make sure the major lines are clearly visible."
    else
        "Now scan your non-dominant hand. This reveals your past karma and soul's innate gifts."

    LazyColumn(Modifier.fillMaxSize().background(BrahmBackground), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            // Step indicator
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Choose Hand", "Dominant", "Non-Dominant", "Report").forEachIndexed { i, label ->
                    val stepNum = if (isDom) 1 else 2
                    val active  = i == stepNum
                    val done    = i < stepNum
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(
                        when { done -> BrahmGold; active -> accentCol; else -> BrahmBorder }
                    ))
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(accentCol.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Text(handEmoji, fontSize = 18.sp, color = accentCol)
                        }
                        Column {
                            Text(handName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = accentCol))
                            Text(karmaLabel, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    }
                    Text(instruction, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, lineHeight = 20.sp))

                    if (uri == null) {
                        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF8F9FA)).border(2.dp, accentCol.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable { onCamera() }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🖐️", fontSize = 32.sp)
                                Text("Tap to photograph your palm", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text("Hold flat under good light", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onCamera, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = accentCol)) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Camera", color = Color.White)
                            }
                            OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BrahmBorder)) {
                                Icon(Icons.Default.Photo, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Gallery")
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp))) {
                            AsyncImage(model = uri, contentDescription = "Palm", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            IconButton(onClick = onReset, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626)), textAlign = TextAlign.Center)

                        if (domResult != null) {
                            // Analysis done — show mini summary + Next button
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(accentCol.copy(alpha = 0.08f)).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = accentCol, modifier = Modifier.size(18.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Reading complete", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = accentCol))
                                    domResult.s("hand_type")?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground)) }
                                }
                            }
                            if (isDom) {
                                Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = accentCol)) {
                                    Text("Scan Non-Dominant Hand →", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                // This should not appear as combined auto-navigates
                                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = accentCol)) {
                                    CircularProgressIndicator(Modifier.size(18.dp), Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp)); Text("Generating combined report...", color = Color.White)
                                }
                            }
                        } else {
                            val btnLabel = if (isDom) "✦ Analyze Dominant Hand" else "☽ Analyze Non-Dominant Hand"
                            Button(onClick = onAnalyze, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = accentCol)) {
                                if (isLoading) { CircularProgressIndicator(Modifier.size(18.dp), Color.White, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text(if (isDom) "Reading dominant hand..." else "Generating full report...", color = Color.White) }
                                else { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(btnLabel, color = Color.White, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BrahmBorder)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Back")
            }
        }
    }
}

// ─── Step 3: Dual Report ──────────────────────────────────────────────────────

@Composable
private fun DualReportStep(dom: JsonObject, nonDom: JsonObject?, combined: JsonObject?, domUri: Uri?, nonDomUri: Uri?, onReset: () -> Unit) {
    val lifeIcons = mapOf("Love & Relationships" to "💕","Career & Purpose" to "🏆","Health & Vitality" to "🌿","Mental Clarity" to "🧠","Wealth & Prosperity" to "✨","Spiritual Growth" to "🕉️")
    val lineColors = mapOf("Heart Line" to Color(0xFFE8650A),"Head Line" to Color(0xFFC8860A),"Life Line" to Color(0xFF7CB87A),"Fate Line" to Color(0xFF7A8BAA),"Sun Line" to Color(0xFFF5C842),"Mercury Line" to Color(0xFF9B8ED4))
    val purple = Color(0xFF7C3AED)

    LazyColumn(Modifier.fillMaxSize().background(BrahmBackground), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ── Combined Synthesis ──
        if (combined != null) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)), border = BorderStroke(1.dp, BrahmGold.copy(alpha = 0.4f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✦", fontSize = 22.sp, color = BrahmGold)
                            Text("Karmic Synthesis", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = onReset, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, null, tint = BrahmMutedForeground, modifier = Modifier.size(16.dp)) }
                        }
                        combined.s("synthesis")?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 20.sp)) }
                    }
                }
            }
            combined.s("karmic_gap")?.let { gap ->
                item {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, BrahmGold.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp)) {
                        Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(BrahmGold))
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Karmic Gap", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = BrahmGold))
                            Text(gap, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 20.sp))
                        }
                    }
                }
            }
            combined.s("soul_mission")?.let { mission ->
                item {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(purple.copy(alpha = 0.06f)).border(1.dp, purple.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🕉️", fontSize = 18.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Soul Mission", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = purple))
                            Text(mission, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 20.sp))
                        }
                    }
                }
            }

            // ── Combined Life Areas ──
            combined.arr("combined_life_areas")?.mapNotNull { try { it.jsonObject } catch (_: Exception) { null } }?.let { areas ->
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Life Areas — Both Hands", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Box(Modifier.size(8.dp).clip(CircleShape).background(purple)); Text("Past", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground)) }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Box(Modifier.size(8.dp).clip(CircleShape).background(BrahmGold)); Text("Present", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground)) }
                            }
                            areas.forEach { area ->
                                val areaName = area.s("area") ?: return@forEach
                                val domScore = area["dominant_score"]?.jsonPrimitive?.intOrNull ?: 5
                                val ndScore  = area["non_dominant_score"]?.jsonPrimitive?.intOrNull ?: 5
                                val insight  = area.s("insight")
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${lifeIcons[areaName] ?: "•"} $areaName", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
                                        Text("$ndScore / $domScore", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                    }
                                    // Past (non-dominant) bar — purple
                                    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(BrahmBorder)) {
                                        Box(Modifier.fillMaxWidth(ndScore / 10f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(purple))
                                    }
                                    // Present (dominant) bar — gold
                                    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(BrahmBorder)) {
                                        Box(Modifier.fillMaxWidth(domScore / 10f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(BrahmGold))
                                    }
                                    insight?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, lineHeight = 18.sp)) }
                                }
                            }
                        }
                    }
                }
            }

            // ── Final message ──
            combined.s("final_message")?.let { msg ->
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = purple.copy(alpha = 0.06f)), border = BorderStroke(1.dp, purple.copy(alpha = 0.2f))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✦ Your Soul's Message", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = purple))
                            Text(msg, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 20.sp, fontStyle = FontStyle.Italic))
                        }
                    }
                }
            }

            // ── Remedies ──
            combined.arr("remedies")?.mapNotNull { try { it.jsonObject } catch (_: Exception) { null } }?.let { remedies ->
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🕯️ Vedic Remedies", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            remedies.forEach { r ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(BrahmGold).padding(top = 5.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        r.s("title")?.let { Text(it, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)) }
                                        r.s("detail")?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, lineHeight = 18.sp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Dominant hand card ──
        item {
            HandResultCard(r = dom, uri = domUri, role = "dominant", accentColor = BrahmGold, lineColors = lineColors, lifeIcons = lifeIcons)
        }

        // ── Non-dominant hand card ──
        nonDom?.let { nd ->
            item {
                HandResultCard(r = nd, uri = nonDomUri, role = "non_dominant", accentColor = purple, lineColors = lineColors, lifeIcons = lifeIcons)
            }
        }

        item {
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BrahmBorder)) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("New Scan")
            }
        }
    }
}

@Composable
private fun HandResultCard(r: JsonObject, uri: Uri?, role: String, accentColor: Color, lineColors: Map<String, Color>, lifeIcons: Map<String, String>) {
    val isDom     = role == "dominant"
    val roleLabel = if (isDom) "✦ Dominant Hand — Present Karma" else "☽ Non-Dominant Hand — Past Karma"
    val lines     = r.arr("lines")?.mapNotNull { try { it.jsonObject } catch (_: Exception) { null } }
    val lifeAreas = r.arr("life_areas")?.mapNotNull { try { it.jsonObject } catch (_: Exception) { null } }
    val strengths = r.arr("strengths")?.mapNotNull { it.jsonPrimitive.contentOrNull }
    val challenges= r.arr("challenges")?.mapNotNull { it.jsonPrimitive.contentOrNull }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                uri?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop) }
                Column(Modifier.weight(1f)) {
                    Text(roleLabel, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = accentColor))
                    r.s("hand_type")?.let { Text(it, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) }
                    r.s("hand_type_vedic")?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground)) }
                }
            }
            r.s("overview")?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 20.sp)) }

            // Lines
            lines?.takeIf { it.isNotEmpty() }?.let { lineList ->
                HorizontalDivider(color = BrahmBorder)
                Text("Palm Lines", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                lineList.forEach { line ->
                    val name  = line.s("name") ?: return@forEach
                    val color = lineColors[name] ?: BrahmGold
                    val score = line["score"]?.jsonPrimitive?.intOrNull ?: 5
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(color)); Spacer(Modifier.width(6.dp))
                            Text(name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                            Text("$score/5", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                        }
                        line.s("interpretation")?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, lineHeight = 18.sp)) }
                    }
                }
            }

            // Life areas
            lifeAreas?.takeIf { it.isNotEmpty() }?.let { areas ->
                HorizontalDivider(color = BrahmBorder)
                Text("Life Areas", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                areas.forEach { area ->
                    val areaName = area.s("area") ?: return@forEach
                    val score    = area["score"]?.jsonPrimitive?.intOrNull ?: 5
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${lifeIcons[areaName] ?: "•"} $areaName", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Box(Modifier.width(80.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(BrahmBorder)) {
                            Box(Modifier.fillMaxWidth(score / 10f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(accentColor))
                        }
                        Spacer(Modifier.width(6.dp)); Text("$score", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    }
                }
            }

            // Strengths + Challenges
            if (!strengths.isNullOrEmpty() || !challenges.isNullOrEmpty()) {
                HorizontalDivider(color = BrahmBorder)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    strengths?.takeIf { it.isNotEmpty() }?.let { list ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Strengths", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A)))
                            list.forEach { Text("✓ $it", style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 18.sp)) }
                        }
                    }
                    challenges?.takeIf { it.isNotEmpty() }?.let { list ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Challenges", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626)))
                            list.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 18.sp)) }
                        }
                    }
                }
            }

            // Summary
            r.s("summary")?.let { summary ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.06f)).padding(10.dp)) {
                    Text(summary, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 20.sp, fontStyle = FontStyle.Italic))
                }
            }
        }
    }
}


// ─── Hand Type Step ───────────────────────────────────────────────────────────

@Composable
private fun HandTypeStep(selectedUri: Uri?, selectedHandTypeId: String?, onSelect: (String) -> Unit, onContinue: () -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(BrahmBackground), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = BrahmMutedForeground) }
                if (selectedUri != null) {
                    AsyncImage(model = selectedUri, contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                }
                Column {
                    Text("Step 1 of ${QUESTIONS.size + 1}", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    Text("What is your hand shape?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                    Text("Look at your palm and fingers to identify your hand type", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF3F4F6))) {
                Box(Modifier.fillMaxWidth(0.1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(BrahmGold))
            }
        }
        items(HAND_TYPES) { ht ->
            val selected = selectedHandTypeId == ht.id
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (selected) BrahmGold.copy(alpha = 0.08f) else Color.White)
                    .border(if (selected) 2.dp else 1.dp, if (selected) BrahmGold else BrahmBorder, RoundedCornerShape(14.dp))
                    .clickable { onSelect(ht.id) }
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(ht.icon, fontSize = 30.sp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(ht.label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            if (selected) Icon(Icons.Default.CheckCircle, null, tint = BrahmGold, modifier = Modifier.size(16.dp))
                        }
                        Text(ht.shape, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        Text(ht.element, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            ht.traits.take(3).forEach { trait ->
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFF3F4F6)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(trait, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = onContinue, enabled = selectedHandTypeId != null, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BrahmGold)) {
                Text("Continue →", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Question Step ────────────────────────────────────────────────────────────

@Composable
private fun QuestionsStep(question: PalmQuestion, selectedUri: Uri?, questionIndex: Int, total: Int, currentSelection: String?, onSelect: (String) -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    val progress = (questionIndex + 1f) / (total + 1f)
    LazyColumn(Modifier.fillMaxSize().background(BrahmBackground), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = BrahmMutedForeground) }
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF3F4F6))) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(BrahmGold))
                }
                Text("Step ${questionIndex + 2} of ${total + 1}", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
            }
        }
        // Palm image reference (if available)
        if (selectedUri != null) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(model = selectedUri, contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Fit)
                        Column(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8F9FA)).border(1.dp, BrahmBorder, RoundedCornerShape(8.dp)).padding(10.dp)) {
                            Text(question.line, style = MaterialTheme.typography.bodySmall.copy(color = question.color, fontWeight = FontWeight.SemiBold))
                            Text(question.instruction, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            Text(question.hint, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground.copy(alpha = 0.7f), fontStyle = FontStyle.Italic))
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(question.line, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = question.color))
                Text("${question.sanskrit} · ${question.instruction}", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
            }
        }
        items(question.options) { opt ->
            val selected = currentSelection == opt.id
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (selected) question.color.copy(alpha = 0.07f) else Color.White)
                    .border(if (selected) 2.dp else 1.dp, if (selected) question.color else BrahmBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelect(opt.id) }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Radio dot
                    Box(Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (selected) question.color else BrahmBorder, CircleShape), contentAlignment = Alignment.Center) {
                        if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(question.color))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(opt.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text(opt.desc, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                        if (selected) {
                            Spacer(Modifier.height(4.dp))
                            Text(opt.meaning, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                            Text("✦ ${opt.vedic}", style = MaterialTheme.typography.labelSmall.copy(color = question.color, fontStyle = FontStyle.Italic, fontSize = 11.sp))
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = onNext, enabled = currentSelection != null, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BrahmGold)) {
                if (questionIndex == total - 1) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Generate My Reading", color = Color.White, fontWeight = FontWeight.SemiBold) }
                else { Text("Next", color = Color.White, fontWeight = FontWeight.SemiBold); Spacer(Modifier.width(4.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

// ─── Report Step ──────────────────────────────────────────────────────────────

@Composable
private fun ReportStep(report: PalmReport, palmUri: Uri?, onReset: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(BrahmBackground), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Header
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, BrahmGold.copy(alpha = 0.3f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (palmUri != null) {
                            AsyncImage(model = palmUri, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = BrahmGold, modifier = Modifier.size(16.dp))
                                Text("Your Hasta Shastra Reading", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PBadge(report.handType.label)
                                PBadge(report.handType.vedic)
                                PBadge(report.handType.element)
                            }
                        }
                        IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Refresh, null, tint = BrahmMutedForeground, modifier = Modifier.size(16.dp)) }
                    }
                    // Vedic note
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BrahmGold.copy(alpha = 0.06f)).border(1.dp, BrahmGold.copy(alpha = 0.2f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                        Text(report.handType.vedicNote, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontStyle = FontStyle.Italic))
                    }
                }
            }
        }

        // Auspicious note banner
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, BrahmGold.copy(alpha = 0.25f), RoundedCornerShape(14.dp)).background(BrahmGold.copy(alpha = 0.05f)).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("✦", fontSize = 18.sp, color = BrahmGold)
                Text(report.auspiciousNote, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
            }
        }

        // Hand Type details + Life Scores (two-section)
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hand Type — ${report.handType.label}", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    // Traits 2-col
                    val traitsChunked = report.handType.traits.chunked(2)
                    traitsChunked.forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { trait ->
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("◆", color = BrahmGold, fontSize = 10.sp)
                                    Text(trait, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontSize = 11.sp))
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    HorizontalDivider(color = BrahmBorder)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoLine("Careers", report.handType.careers)
                        InfoLine("Constitution", report.handType.constitution)
                        InfoLine("Shadow", report.handType.shadow)
                    }
                }
            }
        }

        // Life Area Scores
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Star, null, tint = BrahmGold, modifier = Modifier.size(14.dp))
                        Text("Life Area Scores", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                    report.lifeScores.forEach { ls ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text(ls.icon, fontSize = 13.sp); Text(ls.area, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)) }
                                val sc = if (ls.score >= 8) BrahmGold else if (ls.score >= 6) Color(0xFFE8650A) else BrahmMutedForeground
                                Text("${ls.score}/10", style = MaterialTheme.typography.labelSmall.copy(color = sc, fontWeight = FontWeight.SemiBold))
                            }
                            val sc = if (ls.score >= 8) BrahmGold else if (ls.score >= 6) Color(0xFFE8650A) else BrahmMutedForeground
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF3F4F6))) {
                                Box(Modifier.fillMaxWidth(ls.score / 10f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(sc))
                            }
                        }
                    }
                }
            }
        }

        // Palm Line Readings
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.PanTool, null, tint = BrahmGold, modifier = Modifier.size(16.dp))
                Text("Your Palm Lines", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmGold))
            }
        }
        val selPairs = report.selections.chunked(2)
        selPairs.forEach { pair ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { sel ->
                        val q   = QUESTIONS.find { it.id == sel.questionId }
                        val opt = q?.options?.find { it.id == sel.optionId }
                        if (q != null && opt != null) {
                            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(q.color))
                                        Text(q.line, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                        Text(q.sanskrit, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                                    }
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, BrahmBorder, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(opt.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                    }
                                    Text(opt.meaning, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontSize = 11.sp))
                                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFFF8F9FA)).border(1.dp, BrahmBorder, RoundedCornerShape(6.dp)).padding(8.dp)) {
                                        Text("✦ ${opt.vedic}", style = MaterialTheme.typography.labelSmall.copy(color = q.color, fontStyle = FontStyle.Italic, fontSize = 10.sp))
                                    }
                                }
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // Strengths / Growth Areas / Remedies
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Strengths", style = MaterialTheme.typography.titleSmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold))
                        report.strengths.forEach { s ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("✦", color = BrahmGold, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp)); Text(s, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)) }
                        }
                    }
                }
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Growth Areas", style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFFE8650A), fontWeight = FontWeight.SemiBold))
                        if (report.challenges.isEmpty()) Text("Your palm shows a well-balanced karmic profile with no major challenges. Continue on your current path with devotion.", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontStyle = FontStyle.Italic, fontSize = 11.sp))
                        else report.challenges.forEach { c -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("◆", color = Color(0xFFE8650A), fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp)); Text(c, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)) } }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🕉️ Vedic Remedies", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    report.remedies.forEach { r ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(r.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmForeground))
                            Text(r.detail, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                        }
                        HorizontalDivider(color = BrahmBorder)
                    }
                }
            }
        }

        // Summary
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, BrahmGold.copy(alpha = 0.3f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AutoAwesome, null, tint = BrahmGold, modifier = Modifier.size(16.dp))
                        Text("Your Karmic Path", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmGold))
                    }
                    Text(report.summary, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                    HorizontalDivider(color = BrahmGold.copy(alpha = 0.2f))
                    Text("Based on Hasta Samudrika Shastra. For complete Jyotish analysis, combine with your Kundali chart.", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground.copy(alpha = 0.6f), fontStyle = FontStyle.Italic, fontSize = 11.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item { OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BrahmBorder)) { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("New Reading") } }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── AI Report Step ───────────────────────────────────────────────────────────

// Helper: safely read string from JsonObject key (strips JSON quotes)
private fun JsonObject.s(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
private fun JsonObject.arr(key: String) = try { this[key]?.jsonArray } catch (_: Exception) { null }
private fun JsonObject.obj2(key: String) = try { this[key]?.jsonObject } catch (_: Exception) { null }

@Composable
private fun AiReportStep(r: JsonObject, palmUri: Uri?, onReset: () -> Unit) {
    val lineColors = mapOf("Heart Line" to Color(0xFFE8650A),"Head Line" to Color(0xFFC8860A),"Life Line" to Color(0xFF7CB87A),"Fate Line" to Color(0xFF7A8BAA),"Sun Line" to Color(0xFFF5C842),"Mercury Line" to Color(0xFF9B8ED4))
    val lifeIcons  = mapOf("Love & Relationships" to "💕","Career & Purpose" to "🏆","Health & Vitality" to "🌿","Mental Clarity" to "🧠","Wealth & Prosperity" to "✨","Spiritual Growth" to "🕉️")

    val lifeAreas    = r.arr("life_areas")?.mapNotNull { try { it.jsonObject } catch(_: Exception) { null } }
    val lines        = r.arr("lines")?.mapNotNull { try { it.jsonObject } catch(_: Exception) { null } }
    val mounts       = r.arr("dominant_mounts")?.mapNotNull { try { it.jsonObject } catch(_: Exception) { null } }
    val strengths    = r.arr("strengths")?.mapNotNull { it.jsonPrimitive.contentOrNull }
    val challenges   = r.arr("challenges")?.mapNotNull { it.jsonPrimitive.contentOrNull }
    val remedies     = r.arr("remedies")?.mapNotNull { try { it.jsonObject } catch(_: Exception) { null } }

    LazyColumn(Modifier.fillMaxSize().background(BrahmBackground), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Header
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, BrahmGold.copy(alpha = 0.3f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (palmUri != null) { AsyncImage(model = palmUri, contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop); Spacer(Modifier.width(12.dp)) }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AutoAwesome, null, tint = BrahmGold, modifier = Modifier.size(16.dp))
                                Text("AI Palm Reading", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF6C63FF).copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("AI", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF6C63FF))) }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                r.s("hand_type")?.let { PBadge(it) }
                                r.s("hand_type_vedic")?.let { PBadge(it) }
                                r.s("hand_type_element")?.let { PBadge(it) }
                            }
                        }
                        IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Refresh, null, tint = BrahmMutedForeground, modifier = Modifier.size(16.dp)) }
                    }
                    r.s("hand_type_reading")?.let { reading ->
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BrahmGold.copy(alpha = 0.06f)).padding(10.dp)) {
                            Text(reading, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontStyle = FontStyle.Italic))
                        }
                    }
                }
            }
        }

        // Auspicious note
        r.s("auspicious_note")?.let { note ->
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, BrahmGold.copy(alpha = 0.25f), RoundedCornerShape(14.dp)).background(BrahmGold.copy(alpha = 0.05f)).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✦", fontSize = 18.sp, color = BrahmGold); Text(note, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                }
            }
        }

        // Overview
        r.s("overview")?.let { overview ->
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Overview", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text(overview, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                    }
                }
            }
        }

        // Life area scores
        if (!lifeAreas.isNullOrEmpty()) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.Star, null, tint = BrahmGold, modifier = Modifier.size(14.dp)); Text("Life Area Scores", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)) }
                        lifeAreas.forEach { area ->
                            val areaName = area.s("area") ?: ""
                            val score    = area["score"]?.jsonPrimitive?.intOrNull ?: area["score"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 5
                            val note     = area.s("note")
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text(lifeIcons[areaName] ?: "✦", fontSize = 13.sp); Text(areaName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)) }
                                    val sc = if (score >= 8) BrahmGold else if (score >= 6) Color(0xFFE8650A) else BrahmMutedForeground
                                    Text("$score/10", style = MaterialTheme.typography.labelSmall.copy(color = sc, fontWeight = FontWeight.SemiBold))
                                }
                                val sc = if (score >= 8) BrahmGold else if (score >= 6) Color(0xFFE8650A) else BrahmMutedForeground
                                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF3F4F6))) { Box(Modifier.fillMaxWidth(score / 10f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(sc)) }
                                if (note != null) Text(note, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                            }
                        }
                    }
                }
            }
        }

        // Palm Lines
        if (!lines.isNullOrEmpty()) {
            item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.PanTool, null, tint = BrahmGold, modifier = Modifier.size(16.dp)); Text("Palm Line Analysis", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmGold)) } }
            lines.chunked(2).forEach { pair ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { line ->
                            val name  = line.s("name") ?: ""
                            val color = lineColors[name] ?: BrahmGold
                            Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                                        Text(name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                        line.s("sanskrit")?.let { Text(it, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp)) }
                                    }
                                    line.s("visibility")?.let { Box(Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, BrahmBorder, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(it, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) } }
                                    line.s("observation")?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontStyle = FontStyle.Italic, fontSize = 11.sp)) }
                                    line.s("interpretation")?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontSize = 11.sp)) }
                                    line.s("vedic_note")?.let { Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFFF8F9FA)).border(1.dp, BrahmBorder, RoundedCornerShape(6.dp)).padding(8.dp)) { Text("✦ $it", style = MaterialTheme.typography.labelSmall.copy(color = color, fontStyle = FontStyle.Italic, fontSize = 10.sp)) } }
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Dominant Mounts
        if (!mounts.isNullOrEmpty()) {
            item { Text("Dominant Mounts", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmGold)) }
            mounts.chunked(2).forEach { pair ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { mount ->
                            Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(mount.s("name") ?: "", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                    mount.s("planet")?.let { Text(it, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground)) }
                                    mount.s("condition")?.let { Box(Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, BrahmBorder, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(it.replace("_", " ").replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) } }
                                    mount.s("note")?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontSize = 11.sp)) }
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Strengths / Challenges
        if (!strengths.isNullOrEmpty() || !challenges.isNullOrEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!strengths.isNullOrEmpty()) {
                        Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Strengths", style = MaterialTheme.typography.titleSmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold))
                                strengths.forEach { s -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("✦", color = BrahmGold, fontSize = 10.sp); Text(s, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)) } }
                            }
                        }
                    }
                    if (!challenges.isNullOrEmpty()) {
                        Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Growth Areas", style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFFE8650A), fontWeight = FontWeight.SemiBold))
                                challenges.forEach { c -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("◆", color = Color(0xFFE8650A), fontSize = 10.sp); Text(c, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)) } }
                            }
                        }
                    }
                }
            }
        }

        // Remedies
        if (!remedies.isNullOrEmpty()) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🕉️ Vedic Remedies", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        remedies.forEach { rem ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(rem.s("title") ?: "", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(rem.s("detail") ?: "", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                            }
                            HorizontalDivider(color = BrahmBorder)
                        }
                    }
                }
            }
        }

        // Summary
        r.s("summary")?.let { summary ->
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, BrahmGold.copy(alpha = 0.3f))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.AutoAwesome, null, tint = BrahmGold, modifier = Modifier.size(16.dp)); Text("Your Karmic Path", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmGold)) }
                        Text(summary, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                        HorizontalDivider(color = BrahmGold.copy(alpha = 0.2f))
                        Text("Analyzed by AI · Hasta Samudrika Shastra interpretation", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground.copy(alpha = 0.6f), fontStyle = FontStyle.Italic, fontSize = 11.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item { OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BrahmBorder)) { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("New Reading") } }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── Lines Tab ────────────────────────────────────────────────────────────────

@Composable
private fun LinesTab() {
    val palmLines = QUESTIONS.filter { !it.id.startsWith("mount") }
    var activeId by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(BrahmBackground), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(palmLines) { q ->
            val expanded = activeId == q.id
            Card(modifier = Modifier.fillMaxWidth().clickable { activeId = if (expanded) null else q.id },
                shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(q.color))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(q.line, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text("(${q.sanskrit})", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            }
                            Text(q.hint, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                        }
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = BrahmMutedForeground, modifier = Modifier.size(18.dp))
                    }
                    AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = BrahmBorder)
                            q.options.chunked(2).forEach { pair ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { opt ->
                                        Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF8F9FA)).border(1.dp, BrahmBorder, RoundedCornerShape(10.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, BrahmBorder, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(opt.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) }
                                            Text(opt.meaning, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontSize = 11.sp))
                                            Text("✦ ${opt.vedic}", style = MaterialTheme.typography.labelSmall.copy(color = q.color, fontStyle = FontStyle.Italic, fontSize = 10.sp))
                                        }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── Mounts Tab ───────────────────────────────────────────────────────────────

@Composable
private fun MountsTab() {
    var activeId by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(BrahmBackground), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MOUNTS.chunked(2).forEach { pair ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { mount ->
                        val expanded = activeId == mount.id
                        Card(modifier = Modifier.weight(1f).clickable { activeId = if (expanded) null else mount.id },
                            shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = if (expanded) BorderStroke(1.dp, mount.color.copy(alpha = 0.5f)) else null) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(mount.icon, fontSize = 26.sp, color = mount.color)
                                    Column {
                                        Text(mount.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                        Text("${mount.sanskrit} · ${mount.finger}", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                                    }
                                }
                                Text(mount.well, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp), maxLines = if (expanded) Int.MAX_VALUE else 2)
                                AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        HorizontalDivider(color = BrahmBorder)
                                        MountSection("Well Developed", mount.well, Color(0xFF15803D))
                                        MountSection("Flat", mount.flat, BrahmMutedForeground)
                                        MountSection("Overdeveloped", mount.over, Color(0xFFDC2626))
                                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8F9FA)).border(1.dp, BrahmBorder, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                            Text(mount.vedic, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontStyle = FontStyle.Italic, fontSize = 10.sp))
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.AutoMirrored.Filled.ArrowForward, null, tint = mount.color.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                                    Text(if (expanded) "Collapse" else "Full Reading", style = MaterialTheme.typography.labelSmall.copy(color = mount.color.copy(alpha = 0.7f), fontSize = 10.sp))
                                }
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MountSection(label: String, text: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.SemiBold, fontSize = 10.sp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
    }
}

// ─── Hand Types Tab ───────────────────────────────────────────────────────────

@Composable
private fun HandTypeTab() {
    LazyColumn(Modifier.fillMaxSize().background(BrahmBackground), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(HAND_TYPES) { ht ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(ht.icon, fontSize = 36.sp)
                        Column(Modifier.weight(1f)) {
                            Text(ht.label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("${ht.vedic} · ${ht.shape}", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, BrahmGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(ht.element, style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontSize = 10.sp))
                        }
                    }
                    // Traits 2-col
                    ht.traits.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { trait ->
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("◆", color = BrahmGold, fontSize = 10.sp); Text(trait, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    HorizontalDivider(color = BrahmBorder)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoLine("Careers", ht.careers)
                        InfoLine("Constitution", ht.constitution)
                        InfoLine("Shadow", ht.shadow)
                    }
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF8F9FA)).border(1.dp, BrahmBorder, RoundedCornerShape(10.dp)).padding(10.dp)) {
                        Text(ht.vedicNote, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontStyle = FontStyle.Italic, fontSize = 11.sp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── Small helpers ────────────────────────────────────────────────────────────

@Composable
private fun PBadge(text: String) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(BrahmGold.copy(alpha = 0.1f)).border(1.dp, BrahmGold.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontSize = 10.sp))
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
    }
}
