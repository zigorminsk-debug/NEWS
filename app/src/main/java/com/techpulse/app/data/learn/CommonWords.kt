package com.techpulse.app.data.learn

/**
 * Самые частотные английские слова — «общеизвестные».
 * В режиме обучения они НЕ переводятся: пользователь их и так знает,
 * подсказки показываются только к более редким словам.
 */
object CommonWords {

    val ENGLISH: Set<String> = (
        // местоимения, предлоги, связки
        "the a an and or but if then else when while because since until though although " +
            "as so nor for at by from in into on onto off out over under up down with without " +
            "within between among through during before after around about against along across " +
            "i me my mine myself we us our ours ourselves you your yours yourself yourselves he " +
            "him his himself she her hers herself it its itself they them their theirs themselves " +
            "who whom whose which that this these those what whatever whoever " +
            "someone somebody something anyone anybody anything everyone everybody everything " +
            "no one nobody nothing " +
            // глаголы-связки и модальные
            "be am is are was were been being do does did done doing have has had having " +
            "will would shall should can could may might must ought need needs " +
            // самые частотные глаголы
            "say says said saying tell tells told telling talk talks talked talking speak speaks " +
            "spoke spoken ask asks asked asking answer answers answered give gives gave given " +
            "giving take takes took taken taking make makes made making get gets got getting " +
            "go goes went gone going come comes came coming see sees saw seen seeing look looks " +
            "looked looking know knows knew known knowledges think thinks thought thinking " +
            "find finds found finding keep keeps kept keeping let lets letting leave leaves left " +
            "leaving put puts putting call calls called calling try tries tried trying use uses " +
            "used using want wants wanted wanting like likes liked liking love loves loved " +
            "work works worked working play plays played playing pay pays paid paying buy buys " +
            "bought buying sell sells sold selling send sends sent sending read reads reading " +
            "write writes wrote written writing show shows showed shown showing turn turns " +
            "turned turning start starts started starting stop stops stopped stopping wait waits " +
            "waited waiting meet meets met meeting follow follows followed following lead leads " +
            "led leading build builds built building create creates created creating grow grows " +
            "grew grown growing add adds added adding cut cuts cutting win wins won winning " +
            "lose loses lost losing open opens opened opening close closes closed closing begin " +
            "begins began begun beginning continue continues continued stand stands stood sit " +
            "sits sat sitting hold holds held holding bring brings brought bringing break breaks " +
            "broke broken fall falls fell fallen feel feels felt feeling hear hears heard " +
            "hearing seem seems seemed mean means meant meaning become becomes became becoming " +
            "set sets setting happen happens happened include includes included including allow " +
            "allows allowed offer offers offered consider considers considered support supports " +
            "supported provide provides provided require requires required remain remains " +
            "remained increase increases increased reduce reduces reduced receive receives " +
            "received share shares shared decide decides decided run runs ran running move moves " +
            "moved moving help helps helped helping hope hopes hoped live lives lived change " +
            "changes changed changing watch watches watched watch remember remembers remembered " +
            "forget forgets forgot forgotten believe believes believed understand understands " +
            "understood learn learns learned learning teach teaches taught study studies studied " +
            "please thanks thank yes okay ok " +
            // наречия и оценочные слова
            "very too also even still just only already yet again back here there now today " +
            "yesterday tomorrow always never often sometimes usually really actually probably " +
            "maybe perhaps almost nearly quite rather quite enough soon later early late ago " +
            "however therefore thus meanwhile instead anyway besides moreover furthermore " +
            "together apart away forward forwards backward backwards " +
            // вопросы и количества
            "how why where what who whom whose which when how many much more most less least " +
            "few fewer several all any some each every either neither both other others another " +
            "such same different own first second third fourth fifth sixth seventh eighth ninth " +
            "tenth last next previous once twice thrice zero one two three four five six seven " +
            "eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen " +
            "nineteen twenty thirty forty fifty sixty seventy eighty ninety hundred thousand " +
            "million billion trillion " +
            // время
            "time times year years month months week weeks day days hour hours minute minutes " +
            "second seconds date morning afternoon evening night tonight weekend daily weekly " +
            "monthly yearly annual january february march april may june july august september " +
            "october november december monday tuesday wednesday thursday friday saturday sunday " +
            // прилагательные
            "good better best bad worse worst great greater greatest big bigger biggest small " +
            "smaller smallest large larger largest little less least long longer longest short " +
            "shorter shortest high higher highest low lower lowest new newer newest old older " +
            "oldest young younger youngest easy easier easiest hard harder hardest difficult " +
            "simple clear sure real true false right wrong correct important main major minor " +
            "current recent modern future past present possible impossible likely unlikely " +
            "available common rare similar various single double whole half free full open closed " +
            "public private social local national international global human general special " +
            "specific certain particular different several popular famous official original " +
            "final initial basic normal average huge tiny fast quick slow strong weak rich poor " +
            "happy sad nice fine cool hot cold warm dry wet dark light bright heavy empty busy " +
            "ready safe dangerous " +
            // существительные
            "people person man men woman women child children kid kids family families friend " +
            "friends group groups team teams company companies business businesses world worlds " +
            "country countries city cities state states place places home homes house houses " +
            "room rooms school schools student students teacher teachers class classes course " +
            "courses question questions problem problems idea ideas reason reasons result results " +
            "example examples way ways thing things part parts point points case cases fact facts " +
            "name names word words language languages story stories news information data number " +
            "numbers system systems program programs project projects service services product " +
            "products market markets price prices cost costs value values money cash job jobs " +
            "work life lives death health body bodies hand hands head heads face faces eye eyes " +
            "heart hearts mind minds law laws war wars peace government governments policy " +
            "policies plan plans report reports research science technology internet web site " +
            "sites page pages email mail computer computers software hardware app apps phone " +
            "phones game games book books paper papers music video videos photo photos picture " +
            "pictures image images art design process power energy water fire air land road " +
            "roads street streets car cars door doors line lines form forms level levels area " +
            "areas side sides top bottom front end ends beginning middle kind kinds type types " +
            "list lists table tables user users account accounts " +
            // бренды и собственные имена (переводить не нужно)
            "google apple microsoft meta amazon facebook instagram twitter youtube tiktok " +
            "android ios iphone ipad mac macbook windows linux ubuntu github gitlab openai " +
            "chatgpt gpt tesla spacex nvidia intel amd samsung xiaomi huawei sony netflix " +
            "spotify uber airbnb yandex telegram vk bitcoin ethereum nft ai api"
        )
        .split(' ')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
}
