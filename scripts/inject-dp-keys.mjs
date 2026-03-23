import { readFileSync, writeFileSync } from 'fs';

const en = JSON.parse(readFileSync('src/locales/en.json', 'utf8'));
if (!en.data) en.data = {};
if (!en.data.dp) en.data.dp = {};

const DP = {
  Surya: {
    Mesha:    {theme:'Authority & Self',positive:'Leadership, recognition, government favor',challenge:'Ego conflicts, bone/eye health',tip:'Offer water to Sun daily, Ruby if lagna lord'},
    Vrishabha:{theme:'Wealth & Family',positive:'Income from authority, father blessings',challenge:'Eye/bone issues, ego clashes',tip:'Aditya Hridayam daily, red garnet'},
    Mithuna:  {theme:'Fame & Siblings',positive:'Intellectual recognition, courage',challenge:'Sibling tensions, travel stress',tip:'Gayatri Mantra, donate wheat Sunday'},
    Karka:    {theme:'Home & Property',positive:'Property gains, maternal happiness',challenge:'Mother health, mental stress',tip:'Surya Yantra at home, water offering'},
    Simha:    {theme:'Royalty & Power',positive:'Peak career, fame, leadership',challenge:'Overconfidence, heart health',tip:'Surya Chalisa, donate copper'},
    Kanya:    {theme:'Service & Health',positive:'Systematic success, health improvement',challenge:'Enemies, service disruptions',tip:'Help the poor, Gayatri Mantra'},
    Tula:     {theme:'Partnerships',positive:'Balanced decisions, legal victories',challenge:'Marital tensions, legal disputes',tip:'Sunday charity, Surya namaskar'},
    Vrischika:{theme:'Transformation',positive:'Research, occult abilities',challenge:'Accidents, hidden enemies',tip:'Surya namaskar, control ego'},
    Dhanu:    {theme:'Wisdom & Fortune',positive:'Spiritual growth, higher education',challenge:'Father health, travel fatigue',tip:'Donate wheat Sundays, Aditya mantra'},
    Makara:   {theme:'Discipline & Power',positive:'Political influence, discipline rewards',challenge:'Setbacks, joint/bone issues',tip:'Offer Arghya to Sun, Ruby'},
    Kumbha:   {theme:'Losses & Liberation',positive:'Spiritual insights, foreign success',challenge:'Expenses, isolation',tip:'Meditate, offer to the poor'},
    Meena:    {theme:'Gains & Network',positive:'Income, social influence',challenge:'Eye problems, overextension',tip:'Aditya Hridayam, Sun worship'},
  },
  Chandra: {
    Mesha:    {theme:'Mind & Popularity',positive:'Fame, real estate gains, public support',challenge:'Mental instability, mother health',tip:'Monday fasts, Pearl, milk to Shiva'},
    Vrishabha:{theme:'Wealth & Comfort',positive:'Financial growth, property, luxury',challenge:'Overindulgence, weight gain',tip:'Pearl ring, donate white rice Monday'},
    Mithuna:  {theme:'Communication',positive:'Travel, writing, social success',challenge:'Mental restlessness, anxiety',tip:'Chandra yantra, white flowers'},
    Karka:    {theme:'Home & Happiness',positive:'Family joy, good health, peace',challenge:'Emotional mood swings',tip:'Fast Mondays, drink milk'},
    Simha:    {theme:'Creative Fame',positive:'Creative recognition, popularity',challenge:'Pride, stomach issues',tip:'White flowers to Devi'},
    Kanya:    {theme:'Service & Analysis',positive:'Healthcare success, detailed work',challenge:'Overthinking, worry',tip:'Worship Saraswati, Pearl'},
    Tula:     {theme:'Relationships',positive:'Love, social harmony, gains',challenge:'Indecisiveness',tip:'Pearl, donate white cloth'},
    Vrischika:{theme:'Deep Transformation',positive:'Research, occult mastery',challenge:'Emotional turbulence',tip:'Offer milk to Shiva Lingam'},
    Dhanu:    {theme:'Fortune & Travel',positive:'Spiritual wisdom, good fortune',challenge:'Restlessness, wandering',tip:'White flowers at temple'},
    Makara:   {theme:'Discipline & Career',positive:'Career stability, discipline rewarded',challenge:'Depression, emotional coldness',tip:'Chandra mantra, Monday fasts'},
    Kumbha:   {theme:'Humanitarian Gains',positive:'Social work, abroad earnings',challenge:'Mental fog, isolation',tip:'Offer water to Moon nightly'},
    Meena:    {theme:'Spiritual Gains',positive:'Income, spirituality, foreign travel',challenge:'Escapism, overidealism',tip:'Pearl, worship Devi'},
  },
  Mangal: {
    Mesha:    {theme:'Energy & Leadership',positive:'Courage, sports success, new ventures',challenge:'Accidents, conflicts, blood pressure',tip:'Hanuman Chalisa Tuesdays, Red Coral'},
    Vrishabha:{theme:'Wealth Battles',positive:'Real estate gains, physical strength',challenge:'Financial disputes, health',tip:'Donate red lentils Tuesday'},
    Mithuna:  {theme:'Action & Speech',positive:'Technical skills, bold communication',challenge:'Siblings conflict, accidents',tip:'Kartikeya worship, Red Coral'},
    Karka:    {theme:'Home & Property',positive:'Property gains, strong family',challenge:'Family conflicts, blood disorders',tip:'Tuesday Hanuman puja'},
    Simha:    {theme:'Power & Authority',positive:'Leadership, political power, recognition',challenge:'Arrogance, heat-related issues',tip:'Red Coral if benefic, Mangal mantra'},
    Kanya:    {theme:'Service & Enemies',positive:'Victory over enemies, health discipline',challenge:'Enemies, debt',tip:'Donate red cloth on Tuesdays'},
    Tula:     {theme:'Partnership Battles',positive:'Bold business moves, real estate',challenge:'Marital conflicts, legal issues',tip:'Hanuman worship, Tuesday fast'},
    Vrischika:{theme:'Deep Power',positive:'Research, occult, surgery success',challenge:'Surgeries, injuries',tip:'Karthikeya mantra, Red Coral'},
    Dhanu:    {theme:'Fortune & Drive',positive:'Sports, adventure, higher learning',challenge:'Recklessness, injuries',tip:'Mangal yantra, Tuesday fast'},
    Makara:   {theme:'Ambition & Discipline',positive:'Career authority, disciplined effort',challenge:'Knee/joint issues',tip:'Offer blood-red flowers Tuesday'},
    Kumbha:   {theme:'Gains & Action',positive:'Income through action, technical jobs',challenge:'Accidents, unexpected costs',tip:'Red Coral, Hanuman Chalisa'},
    Meena:    {theme:'Losses & Moksha',positive:'Spiritual drive, hidden strengths',challenge:'Accidents, expenditure',tip:'Hanuman puja, avoid rash decisions'},
  },
  Rahu: {
    Mesha:    {theme:'Ambition & Confusion',positive:'Foreign connections, unique leadership',challenge:'Ego illusions, impulsiveness',tip:'Rahu mantra, donate coconut Saturday'},
    Vrishabha:{theme:'Material Desires',positive:'Wealth through unconventional means',challenge:'Attachment, overindulgence',tip:'Hessonite, donate black items'},
    Mithuna:  {theme:'Intellect & Illusion',positive:'Tech, media, foreign communication',challenge:'Deceptive speech, nervousness',tip:'Rahu yantra, donate urad dal'},
    Karka:    {theme:'Home & Illusion',positive:'Foreign property, maternal karma',challenge:'Mental confusion, family disruption',tip:'Chant Rahu mantra, Moon rituals'},
    Simha:    {theme:'Power & Ego',positive:'Fame through unconventional means',challenge:'Ego conflicts, heart troubles',tip:'Rahu mantra, donate sesame'},
    Kanya:    {theme:'Service & Karma',positive:'Healthcare, service gains abroad',challenge:'Hidden enemies, confusion at work',tip:'Serve the underprivileged'},
    Tula:     {theme:'Relationships & Karma',positive:'Unusual partnerships, foreign gains',challenge:'Relationship karma',tip:'Shukra mantra, hessonite'},
    Vrischika:{theme:'Deep Karma',positive:'Occult mastery, research',challenge:'Obsession, hidden dangers',tip:'Rahu yantra, Saturday fast'},
    Dhanu:    {theme:'Foreign Fortune',positive:'Travel, foreign education',challenge:'Religious confusion',tip:'Donate to educational causes'},
    Makara:   {theme:'Ambition & Power',positive:'Political gains, career abroad',challenge:'Ruthlessness',tip:'Rahu mantra, donate'},
    Kumbha:   {theme:'Innovation',positive:'Tech success, humanitarian causes',challenge:'Eccentric behavior',tip:'Hessonite, Rahu puja'},
    Meena:    {theme:'Spiritual Confusion',positive:'Spiritual intuition, foreign travel',challenge:'Escapism, drug tendencies',tip:'Rahu mantra, spiritual practice'},
  },
  Ketu: {
    Mesha:    {theme:'Detachment & Past Karma',positive:'Spiritual insight, martial arts',challenge:'Headaches, impulsiveness',tip:'Ketu mantra, donate blanket'},
    Vrishabha:{theme:'Detachment from Wealth',positive:'Spiritual wealth, past-life skills',challenge:'Financial detachment, stubbornness',tip:'Cat Eye, donate food'},
    Mithuna:  {theme:'Past Intellect',positive:'Writing, spiritual communication',challenge:'Mental detachment, confusion',tip:'Ketu yantra, donate gray cloth'},
    Karka:    {theme:'Emotional Detachment',positive:'Psychic abilities, spiritual home',challenge:'Family alienation',tip:'Ketu mantra, water offering'},
    Simha:    {theme:'Fame & Detachment',positive:'Spiritual fame, royal past karma',challenge:'Ego dissolution',tip:'Ganesh puja, Ketu mantra'},
    Kanya:    {theme:'Past Service Karma',positive:'Health mastery, analytical depth',challenge:'Critical detachment',tip:'Serve animals, Cat Eye'},
    Tula:     {theme:'Relationship Karma',positive:'Past-life partner connections',challenge:'Relationship disconnection',tip:'Ketu mantra, spiritual practice'},
    Vrischika:{theme:'Deep Past Karma',positive:'Occult mastery, kundalini awakening',challenge:'Obsession, self-undoing',tip:'Ketu yantra, fast Tuesdays'},
    Dhanu:    {theme:'Wisdom & Renunciation',positive:'Spiritual teacher, foreign guru',challenge:'Dogmatic past beliefs',tip:'Meditate, donate to ashrams'},
    Makara:   {theme:'Past Discipline',positive:'Career karma clearing, authority',challenge:'Career detachment',tip:'Cat Eye, Ketu mantra'},
    Kumbha:   {theme:'Past Innovation',positive:'Humanitarian causes, unique insight',challenge:'Social detachment',tip:'Ketu mantra, donate'},
    Meena:    {theme:'Liberation',positive:'Moksha path, spiritual liberation',challenge:'Confusion, loss',tip:'Ketu puja, spiritual retreat'},
  },
  Shani: {
    Mesha:    {theme:'Discipline vs Ego',positive:'Leadership through perseverance',challenge:'Delays, accidents, ego blocks',tip:'Shani mantra, donate mustard oil Saturday'},
    Vrishabha:{theme:'Wealth through Hard Work',positive:'Slow but steady financial growth',challenge:'Financial obstacles, health',tip:'Blue Sapphire if benefic, Shani puja'},
    Mithuna:  {theme:'Disciplined Communication',positive:'Systematic writing, research',challenge:'Communication barriers, nervous issues',tip:'Donate black sesame Saturday'},
    Karka:    {theme:'Home Karma',positive:'Property through discipline',challenge:'Family burden, mother health',tip:'Shani yantra, Saturday fasts'},
    Simha:    {theme:'Power & Restriction',positive:'Authority through discipline',challenge:'Career delays, health',tip:'Hanuman Chalisa, blue sapphire'},
    Kanya:    {theme:'Service & Karma',positive:'Medical/technical success',challenge:'Enemies, workplace issues',tip:'Serve the poor, Shani puja'},
    Tula:     {theme:'Justice & Partnerships',positive:'Legal victories, disciplined partnerships',challenge:'Slow relationship growth',tip:'Blue Sapphire, donate on Saturdays'},
    Vrischika:{theme:'Deep Transformation',positive:'Research, discipline, spiritual depth',challenge:'Chronic health, delays',tip:'Shani mantra, fast Saturdays'},
    Dhanu:    {theme:'Discipline & Fortune',positive:'Structured wisdom, foreign gains',challenge:'Father health, travel restriction',tip:'Shani puja, donate'},
    Makara:   {theme:'Peak Discipline',positive:'Maximum career success through hard work',challenge:'Overwork, isolation',tip:'Blue Sapphire, offer sesame oil'},
    Kumbha:   {theme:'Humanitarian Discipline',positive:'Social reform, career in service',challenge:'Isolation, expenses',tip:'Shani yantra, serve needy'},
    Meena:    {theme:'Karma & Liberation',positive:'Spiritual discipline, foreign work',challenge:'Hidden enemies, losses',tip:'Meditate, Shani mantra'},
  },
  Guru: {
    Mesha:    {theme:'Fortune & Expansion',positive:'Legal wins, teaching, children blessed',challenge:'Weight gain, overoptimism',tip:'Yellow Sapphire, Thursday fast'},
    Vrishabha:{theme:'Wealth & Wisdom',positive:'Financial boom, family happiness',challenge:'Overindulgence',tip:'Donate yellow sweets Thursday'},
    Mithuna:  {theme:'Intellect & Fortune',positive:'Higher education, publishing, travel',challenge:'Liver/fat issues',tip:'Guru mantra, Yellow Sapphire'},
    Karka:    {theme:'Home & Blessings',positive:'Family blessings, property, children',challenge:'Overprotection',tip:'Brihaspati Vrata Thursdays'},
    Simha:    {theme:'Royalty & Dharma',positive:'Leadership recognition, spiritual authority',challenge:'Pride in knowledge',tip:'Worship Dakshinamurti'},
    Kanya:    {theme:'Service & Analysis',positive:'Teaching, medical success',challenge:'Over-analysis, weight',tip:'Yellow Sapphire, donate turmeric'},
    Tula:     {theme:'Relationships',positive:'Marriage blessings, business partnerships',challenge:'Relationship overextension',tip:'Thursday fast, yellow cloth donation'},
    Vrischika:{theme:'Transformation',positive:'Research, spiritual depth, healing',challenge:'Hidden enemies, liver',tip:'Guru yantra, Brihaspati Vrata'},
    Dhanu:    {theme:'Peak Fortune',positive:'Maximum expansion, travel, wisdom',challenge:'Overconfidence, travel excess',tip:'Yellow Sapphire, Thursday worship'},
    Makara:   {theme:'Discipline & Wisdom',positive:'Structured growth, senior recognition',challenge:'Delayed rewards',tip:'Donate yellow on Thursdays'},
    Kumbha:   {theme:'Humanitarian',positive:'Social causes, innovation, income',challenge:'Unconventional path',tip:'Feed Brahmin on Thursdays'},
    Meena:    {theme:'Spiritual Fortune',positive:'Spiritual growth, abroad success',challenge:'Idealism, overgiving',tip:'Worship Brihaspati, Yellow Sapphire'},
  },
  Shukra: {
    Mesha:    {theme:'Relationships & Luxury',positive:'Love, beauty, artistic success',challenge:'Laziness, relationship turbulence',tip:'Diamond/White Sapphire, Friday fast'},
    Vrishabha:{theme:'Wealth & Pleasure',positive:'Maximum luxury, love, financial growth',challenge:'Overindulgence',tip:'Worship Lakshmi on Fridays'},
    Mithuna:  {theme:'Arts & Communication',positive:'Artistic/media success, pleasant speech',challenge:'Fickleness in relationships',tip:'Diamond, Friday Lakshmi puja'},
    Karka:    {theme:'Home & Beauty',positive:'Beautiful home, happy family',challenge:'Emotional in love',tip:'Offer white flowers to Lakshmi'},
    Simha:    {theme:'Fame & Creativity',positive:'Artistic fame, recognition, romance',challenge:'Vanity, ego',tip:'Shukra mantra, Friday worship'},
    Kanya:    {theme:'Service & Elegance',positive:'Medical aesthetics, fashion, health',challenge:'Perfectionistic in love',tip:'Donate white sweets Friday'},
    Tula:     {theme:'Peak Relationships',positive:'Maximum love, business partnerships',challenge:'Overromanticism',tip:'Diamond, worship Lakshmi'},
    Vrischika:{theme:'Deep Desires',positive:'Hidden pleasures, research, depth',challenge:'Jealousy, secret affairs',tip:'Shukra mantra, Friday fast'},
    Dhanu:    {theme:'Fortune & Beauty',positive:'Travel, luxury, spiritual beauty',challenge:'Overindulgence abroad',tip:'White Sapphire, Lakshmi puja'},
    Makara:   {theme:'Discipline & Luxury',positive:'Career through arts/beauty, practical love',challenge:'Cold in relationships',tip:'Friday fast, donate white'},
    Kumbha:   {theme:'Humanitarian Beauty',positive:'Social art, unique relationships',challenge:'Unconventional love issues',tip:'Shukra yantra, Friday worship'},
    Meena:    {theme:'Spiritual Love',positive:'Devotional art, spiritual relationships',challenge:'Escapism in love',tip:'Offer white flowers to Devi'},
  },
  Budh: {
    Mesha:    {theme:'Intelligence & Action',positive:'Business acumen, communication',challenge:'Nervous system, indecision',tip:'Emerald if benefic, feed green parrots'},
    Vrishabha:{theme:'Wealth & Speech',positive:'Business gains, eloquent communication',challenge:'Skin issues, overthinking',tip:'Saraswati worship, Emerald'},
    Mithuna:  {theme:'Intellect & Skills',positive:'Peak intellectual power, media success',challenge:'Nervousness, skin issues',tip:'Wednesday fasts, green moong donation'},
    Karka:    {theme:'Mind & Home',positive:'Education, writing from home',challenge:'Indecisiveness, digestive issues',tip:'Budha yantra, Saraswati mantra'},
    Simha:    {theme:'Fame & Intellect',positive:'Renowned speaker/writer, recognition',challenge:'Ego in communication, pride',tip:'Recite Budha Ashtakam'},
    Kanya:    {theme:'Service & Analysis',positive:'Maximum intellectual power, health jobs',challenge:'Overthinking, perfectionism',tip:'Emerald, feed cows green grass'},
    Tula:     {theme:'Partnerships',positive:'Business partnerships, balanced intellect',challenge:'Indecisiveness in relationships',tip:'Wednesday worship, green charity'},
    Vrischika:{theme:'Research & Depth',positive:'Research, occult studies, investigative',challenge:'Suspicious nature, nervous disorders',tip:'Saraswati puja'},
    Dhanu:    {theme:'Wisdom & Learning',positive:'Academic success, philosophical writing',challenge:'Scattered focus',tip:'Donate books on Wednesdays'},
    Makara:   {theme:'Practical Intellect',positive:'Business systems, financial planning',challenge:'Dry communication',tip:'Emerald, Wednesday fast'},
    Kumbha:   {theme:'Humanitarian Intellect',positive:'Social media, tech innovation',challenge:'Eccentric thinking',tip:'Budha mantra, green charity'},
    Meena:    {theme:'Spiritual Wisdom',positive:'Creative writing, imagination, spirituality',challenge:'Confusion, impracticality',tip:'Donate green cloth Wednesday'},
  },
};

let count = 0;
for (const planet of Object.keys(DP)) {
  for (const rashi of Object.keys(DP[planet])) {
    const d = DP[planet][rashi];
    const pk = planet.toLowerCase();
    const rk = rashi.toLowerCase();
    en.data.dp[`${pk}_${rk}_theme`] = d.theme;
    en.data.dp[`${pk}_${rk}_pos`]   = d.positive;
    en.data.dp[`${pk}_${rk}_cha`]   = d.challenge;
    en.data.dp[`${pk}_${rk}_tip`]   = d.tip;
    count += 4;
  }
}

writeFileSync('src/locales/en.json', JSON.stringify(en, null, 2));
console.log(`Added ${count} dasha prediction keys`);
