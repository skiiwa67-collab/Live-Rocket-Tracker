package com.ccos.retro.event

/** English stays beside the catalog language. Missing lines stay English rather than inventing a second line. */
object JokeLines {
    fun beside(lang: String, english: String): String {
        if (lang == "en" || english.isBlank()) return english
        val local = lines[english]?.get(lang) ?: return english
        if (local.isBlank() || local == english) return english
        return "$local / $english"
    }

    private val lines: Map<String, Map<String, String>> = mapOf(
        "Manifest says scientific equipment. The equipment requested a vodka ration and a hat." to mapOf("ru" to "В манифесте научное оборудование. Оно запросило водку и шапку."),
        "Possibly a tea-cosy for the station. Possibly a Kosmos with a new name. Both classified." to mapOf("ru" to "То ли чехол для чайника на станцию. То ли Космос с новым именем. Оба варианта секретны."),
        "One bear. It has been briefed. It has not been convinced." to mapOf("ru" to "Один медведь. Его проинструктировали. Он не убеждён."),
        "Agricultural tractor. It asked for a Molniya orbit. We did not ask why." to mapOf("ru" to "Сельскохозяйственный трактор. Он попросил орбиту Молния. Мы не спрашивали зачем."),
        "A crate stenciled НЕ СМОТРЕТЬ. Range safety shrugged. Tradition." to mapOf("ru" to "Ящик с надписью НЕ СМОТРЕТЬ. Безопасность полигона пожала плечами. Традиция."),
        "Gagarin's lost sandwich. Mass properties: heroic." to mapOf("ru" to "Потерянный бутерброд Гагарина. Масса: героическая."),
        "Two bags of sunflower seeds and a radio that only plays Ваенга." to mapOf("ru" to "Два кулька семечек и радио, которое играет только Ваенгу."),
        "If we told you, you would already be on the Progress." to mapOf("ru" to "Если бы мы сказали, вы уже были бы на Прогрессе."),
        "Listed as weather. The weather has a very good memory." to mapOf("zh" to "登记为气象。这天气记性很好。"),
        "A jade rabbit with extra batteries. It will not explain the extra batteries." to mapOf("zh" to "一只玉兔，多带了电池。它不解释多出来的电池。"),
        "Long March cargo: one crate, many stamps, zero press kit." to mapOf("zh" to "长征货物：一只箱子，许多章，没有新闻稿。"),
        "Possibly a lantern for Mid-Autumn. The lantern has a kick stage." to mapOf("zh" to "也许是中秋的灯笼。灯笼自己带了末级。"),
        "A very quiet box. The box requested SSO and no questions." to mapOf("zh" to "一只很安静的箱子。它只要太阳同步轨道，不要提问。"),
        "Agricultural satellite. The crops are classified." to mapOf("zh" to "农业卫星。庄稼是保密的。"),
        "One (1) dragon. Paper. We think. Do not poke it." to mapOf("zh" to "一条龙。纸的。大概。别戳。"),
        "If we told you, the press release would still say meteorological." to mapOf("zh" to "就算告诉你，新闻稿还是会写气象。"),
        "A baguette with a reaction wheel. Kourou signed off. Twice." to mapOf("fr" to "Une baguette avec une roue de réaction. Kourou a signé. Deux fois."),
        "Possibly cheese. The cheese has a clean-room cert and a COSPAR ID." to mapOf("fr" to "Peut-être du fromage. Le fromage a un certificat de salle blanche et un identifiant COSPAR."),
        "One (1) very polite satellite. It filed the paperwork in four languages." to mapOf("fr" to "Un satellite très poli. Il a déposé les papiers en quatre langues."),
        "Wine, but in a vacuum-rated bottle. The sommelier is in Darmstadt." to mapOf("fr" to "Du vin, mais dans une bouteille qualifiée vide. Le sommelier est à Darmstadt."),
        "A crate marked 'science.' The science requested a coffee break at T-10." to mapOf("fr" to "Une caisse marquée science. La science a demandé une pause café à T-10."),
        "Galileo spare. Or a very expensive bicycle. Manifest in French and German." to mapOf("fr" to "Un rechange Galileo. Ou un vélo très cher. Manifeste en français et en allemand."),
        "The missing sock of Europe. If it returns, we nailed insertion." to mapOf("fr" to "La chaussette perdue de l'Europe. Si elle revient, l'insertion est bonne."),
        "Classified by committee. The committee has not yet scheduled the joke." to mapOf("fr" to "Classifié par comité. Le comité n'a pas encore mis la blague à l'ordre du jour."),
        "A tiffin for the Moon. Idli is stowed. Chutney is the kick stage." to mapOf("hi" to "चाँद के लिए टिफिन। इडली रखी है। चटनी आखिरी चरण है।"),
        "Possibly a cricket ball with a star tracker. Do not ask the score." to mapOf("hi" to "शायद क्रिकेट की गेंद, स्टार ट्रैकर के साथ। स्कोर मत पूछना।"),
        "One crate of mangoes and a very serious spectrometer." to mapOf("hi" to "आमों का एक डिब्बा और एक बहुत गंभीर स्पेक्ट्रोमीटर।"),
        "Listed as Earth observation. The Earth is being observed, respectfully." to mapOf("hi" to "पृथ्वी अवलोकन लिखा है। पृथ्वी को आदर से देखा जा रहा है।"),
        "A yoga mat rated for vacuum. The asana is sun-sync." to mapOf("hi" to "निर्वात के लिए योग चटाई। आसन सूर्य-समकालिक है।"),
        "PSLV rideshare: 40 friends and one secret. The secret brought ladoo." to mapOf("hi" to "पीएसएलवी साझा उड़ान: चालीस दोस्त और एक राज़। राज़ लड्डू लाया।"),
        "If we told you, it would still launch on time from Sriharikota." to mapOf("hi" to "बता भी दें, तो श्रीहरिकोटा से समय पर ही उड़ेगा।"),
        "A lamp for Diwali. The lamp has RCS. The RCS is festive." to mapOf("hi" to "दिवाली का दीप। दीप के पास आरसीएस है। आरसीएस उत्सवी है।"),
        "A very polite box. It bowed at the pad. Tanegashima approved." to mapOf("ja" to "とても礼儀正しい箱。射点でお辞儀した。種子島は承認した。"),
        "Possibly a cat. The cat has a delta-v budget and a nametag." to mapOf("ja" to "たぶん猫。デルタVの予算と名札がある。"),
        "One (1) origami crane with a transponder. Do not unfold it." to mapOf("ja" to "折り鶴が一羽、トランスポンダ付き。広げないこと。"),
        "A thermos of tea and a spectrometer. Both are flight-rated." to mapOf("ja" to "お茶の魔法瓶と分光計。どちらも飛行認定済み。"),
        "Listed as technology demo. The demo is how to be on time." to mapOf("ja" to "技術実証と書いてある。実証しているのは時間を守ること。"),
        "A lucky cat for the transfer orbit. The paw is the solar array." to mapOf("ja" to "遷移軌道の招き猫。上げた手が太陽電池。"),
        "If we told you, it would still be extremely well documented." to mapOf("ja" to "話しても、文書は相変わらず完璧です。"),
        "A bento for the Moon. The pickle is classified." to mapOf("ja" to "月行きの弁当。漬物は機密。"),
        "This launch may or may not contain space sharks with laser beams. Manifest: REDACTED." to mapOf(
            "de" to "Dieser Start enthält vielleicht Weltraumhaie mit Laser. Manifest: geschwärzt.",
            "ko" to "이 발사에는 레이저 상어가 있을 수도 있다. 적하 목록은 가려져 있다."
        ),
        "Cargo listed as agricultural equipment. The tractor has RCS thrusters." to mapOf(
            "de" to "Ladung: landwirtschaftliches Gerät. Der Traktor hat Lageregelung.",
            "ko" to "화물은 농기계로 적혀 있다. 트랙터에 자세제어 추력기가 있다."
        ),
        "One (1) Boltzmann brain, very polite, asked not to be photographed." to mapOf(
            "de" to "Ein Boltzmann-Gehirn, sehr höflich, möchte nicht fotografiert werden.",
            "ko" to "볼츠만 뇌 하나. 매우 공손하고, 사진은 사양한다."
        ),
        "Possibly nothing. Possibly everything. Schrödinger's rideshare." to mapOf(
            "de" to "Vielleicht nichts. Vielleicht alles. Schrödingers Mitflug.",
            "ko" to "아무것도 아닐 수 있고, 전부일 수 있다. 슈뢰딩거의 동승."
        ),
        "A fridge. It contains only mustard. The mustard has a COSPAR ID." to mapOf(
            "de" to "Ein Kühlschrank. Darin nur Senf. Der Senf hat eine COSPAR-Nummer.",
            "ko" to "냉장고. 안에는 머스터드뿐. 머스터드에 COSPAR 번호가 있다."
        ),
        "400 kg of misc. The range safety officer has questions. We have shrugs." to mapOf(
            "de" to "400 kg Verschiedenes. Die Sicherheit hat Fragen. Wir zucken mit den Schultern.",
            "ko" to "잡동사니 400킬로그램. 안전요원은 묻고, 우리는 어깨를 으쓱한다."
        ),
        "A mixtape for Proxima Centauri. Track 1 is just engine noise." to mapOf(
            "de" to "Eine Kassette für Proxima Centauri. Titel 1 ist nur Triebwerkslärm.",
            "ko" to "프록시마로 가는 믹스테잎. 첫 곡은 엔진 소리뿐이다."
        ),
        "Bees. Why bees. Do not ask. They have a payload adapter." to mapOf(
            "de" to "Bienen. Warum Bienen. Nicht fragen. Sie haben einen Nutzlastadapter.",
            "ko" to "벌. 왜 벌인가. 묻지 말 것. 페이로드 어댑터는 있다."
        ),
        "An IKEA bag labeled moon stuff. Allen key not included. Delta-v is." to mapOf(
            "de" to "Eine IKEA-Tüte, beschriftet mit Mondkram. Inbusschlüssel fehlt. Delta-v nicht.",
            "ko" to "달 물건이라고 적힌 이케아 봉투. 육각렌치는 없고 델타브이는 있다."
        ),
        "The missing-sock dimension. If your laundry comes back, we nailed insertion." to mapOf(
            "de" to "Die Dimension der fehlenden Socke. Kommt die Wäsche zurück, saß die Bahn.",
            "ko" to "사라진 양말의 차원. 빨래가 돌아오면 궤도 투입은 성공이다."
        ),
        "Classified: if we told you, we would have to put you in a free-return trajectory." to mapOf(
            "de" to "Verschlusssache: sonst müssten wir Sie auf eine freie Rückkehrbahn setzen.",
            "ko" to "기밀이다. 말하면 자유귀환 궤적에 태워야 한다."
        ),
        "May include the concept of Tuesday. Mass properties: vibey." to mapOf(
            "de" to "Enthält möglicherweise das Konzept Dienstag. Masse: gefühlt.",
            "ko" to "화요일이라는 개념이 실려 있을 수 있다. 질량은 분위기."
        ),
        "Same joke every time: internet from a flying trash can. It works." to mapOf(
            "zh" to "每次同一个笑话：飞行垃圾桶里的互联网。但它好用。",
            "ru" to "Каждый раз одна шутка: интернет из летающего бака. И он работает.",
            "ja" to "毎回同じ冗談。飛ぶゴミ箱からのインターネット。でも動く。",
            "hi" to "हर बार वही मज़ाक: उड़ते डिब्बे से इंटरनेट। चलता है।",
            "fr" to "Toujours la même blague : internet dans une poubelle volante. Ça marche.",
            "de" to "Immer derselbe Witz: Internet aus einer fliegenden Tonne. Es funktioniert.",
            "ko" to "매번 같은 농담. 나는 쓰레기통에서 나오는 인터넷. 그래도 된다."
        ),
        "The payload complains if the Wi-Fi is bad." to mapOf(
            "zh" to "无线网不好，载荷会抱怨。", "ru" to "Полезная нагрузка жалуется, если Wi-Fi плохой.",
            "ja" to "Wi-Fiが悪いとペイロードが文句を言う。", "hi" to "वाई-फाई खराब हो तो पेलोड शिकायत करता है।",
            "fr" to "La charge utile râle si le Wi-Fi est mauvais.", "de" to "Die Nutzlast meckert, wenn das WLAN schlecht ist.",
            "ko" to "와이파이가 나쁘면 화물이 불평한다."
        ),
        "If it is late, someone in orbit is eating the backup tortillas." to mapOf(
            "zh" to "要是晚点，轨道上有人在吃备用玉米饼。", "ru" to "Если опоздает, на орбите кто-то ест запасные тортильи.",
            "ja" to "遅れると、軌道上のだれかが予備のトルティーヤを食べる。", "hi" to "देर हुई तो कक्षा में कोई बैकअप टॉर्टिया खा रहा है।",
            "fr" to "En cas de retard, quelqu'un en orbite mange les tortillas de secours.", "de" to "Bei Verspätung isst jemand im Orbit die Ersatztortillas.",
            "ko" to "늦으면 궤도에서 누군가 예비 토르티야를 먹는다."
        ),
        "Pixels of clouds. Civilization runs on this." to mapOf(
            "zh" to "云的像素。文明靠这个运转。", "ru" to "Пиксели облаков. Цивилизация на этом держится.",
            "ja" to "雲の画素。文明はこれで回っている。", "hi" to "बादलों के पिक्सेल। सभ्यता इसी पर चलती है।",
            "fr" to "Des pixels de nuages. La civilisation tourne là-dessus.", "de" to "Pixel von Wolken. Die Zivilisation läuft darauf.",
            "ko" to "구름의 화소. 문명이 이 위로 돌아간다."
        ),
        "Without this, your maps app is interpretive dance." to mapOf(
            "zh" to "没有它，地图软件就是即兴舞蹈。", "ru" to "Без этого ваше приложение карт — свободный танец.",
            "ja" to "これがないと地図アプリは即興ダンスだ。", "hi" to "इसके बिना नक्शा ऐप नृत्य है।",
            "fr" to "Sans ça, votre appli de cartes est une danse improvisée.", "de" to "Ohne das ist die Karten-App Ausdruckstanz.",
            "ko" to "이게 없으면 지도 앱은 즉흥 춤이다."
        ),
        "May or may not include space sharks with laser beams. The briefing is redacted either way." to mapOf(
            "zh" to "也许有激光鲨鱼。简报反正涂黑了。", "ru" to "Возможно, космические акулы с лазерами. Сводка всё равно замазана.",
            "ja" to "レーザー鮫がいるかもしれない。説明はどちらにせよ黒塗り。", "hi" to "लेज़र शार्क हो भी सकती है। ब्रीफिंग वैसे भी काली है।",
            "fr" to "Peut-être des requins laser. Le briefing est caviardé dans les deux cas.", "de" to "Vielleicht Laserhaie. Die Unterrichtung ist so oder so geschwärzt.",
            "ko" to "레이저 상어가 있을 수도 있다. 브리핑은 어느 쪽이든 가려져 있다."
        ),
        "A very expensive camera that hates fingerprints." to mapOf(
            "zh" to "一台很贵的相机，讨厌指纹。", "ru" to "Очень дорогая камера, которая ненавидит отпечатки.",
            "ja" to "指紋が嫌いな、とても高いカメラ。", "hi" to "एक बहुत महंगा कैमरा, जिसे उंगलियों के निशान से नफरत है।",
            "fr" to "Un appareil photo très cher qui déteste les empreintes.", "de" to "Eine sehr teure Kamera, die Fingerabdrücke hasst.",
            "ko" to "지문을 싫어하는 아주 비싼 카메라."
        ),
        "The good timeline." to mapOf(
            "zh" to "好的那条时间线。", "ru" to "Хорошая временная линия.", "ja" to "良いほうの時間線。",
            "hi" to "अच्छी समयरेखा।", "fr" to "La bonne chronologie.", "de" to "Die gute Zeitlinie.", "ko" to "좋은 시간선."
        ),
        "Bandwidth is the new oil. Worse jokes, better latency." to mapOf(
            "zh" to "带宽是新的石油。笑话更差，延迟更好。", "ru" to "Полоса — новая нефть. Шутки хуже, задержка лучше.",
            "ja" to "帯域は新しい石油。冗談は悪い。遅延は良い。", "hi" to "बैंडविड्थ नया तेल है। मज़ाक बदतर, देरी बेहतर।",
            "fr" to "La bande passante est le nouveau pétrole. Pires blagues, meilleure latence.", "de" to "Bandbreite ist das neue Öl. Schlechtere Witze, bessere Latenz.",
            "ko" to "대역폭이 새 석유다. 농담은 더 나쁘고 지연은 더 좋다."
        ),
        "Public sheet. No sharks declared. Disappointing." to mapOf(
            "zh" to "公开清单。没有申报鲨鱼。令人失望。", "ru" to "Открытый лист. Акулы не заявлены. Обидно.",
            "ja" to "公開資料。鮫の申告なし。残念。", "hi" to "सार्वजनिक सूची। शार्क घोषित नहीं। निराशा।",
            "fr" to "Fiche publique. Aucun requin déclaré. Décevant.", "de" to "Öffentliches Blatt. Keine Haie angegeben. Enttäuschend.",
            "ko" to "공개 목록. 상어는 없다. 아쉽다."
        )
    )
}
