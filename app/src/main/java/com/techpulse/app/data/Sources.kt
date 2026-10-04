package com.techpulse.app.data

/**
 * Источники данных приложения:
 *  - NEWS     — RSS-ленты IT-новостей (русские и мировые);
 *  - RESOURCES — каталог полезных сайтов и сервисов для IT-специалистов.
 */
object Sources {

    val NEWS: List<NewsSource> = listOf(
        NewsSource(
            id = "habr",
            name = "Хабр",
            rssUrl = "https://habr.com/ru/rss/best/daily/?fl=ru",
            siteUrl = "https://habr.com/",
            colorHex = 0xFF5B8DFF,
            lang = "ru"
        ),
        NewsSource(
            id = "3dnews",
            name = "3DNews",
            rssUrl = "https://3dnews.ru/news/rss/",
            siteUrl = "https://3dnews.ru/",
            colorHex = 0xFFFF7A59,
            lang = "ru"
        ),
        NewsSource(
            id = "opennet",
            name = "OpenNET",
            rssUrl = "https://www.opennet.ru/opennews/opennews_all_utf.rss",
            siteUrl = "https://www.opennet.ru/",
            colorHex = 0xFFFFC857,
            lang = "ru"
        ),
        NewsSource(
            id = "hn",
            name = "Hacker News",
            rssUrl = "https://hnrss.org/frontpage",
            siteUrl = "https://news.ycombinator.com/",
            colorHex = 0xFFFF6B35,
            lang = "en"
        ),
        NewsSource(
            id = "techcrunch",
            name = "TechCrunch",
            rssUrl = "https://techcrunch.com/feed/",
            siteUrl = "https://techcrunch.com/",
            colorHex = 0xFF3DFFA2,
            lang = "en"
        ),
        NewsSource(
            id = "verge",
            name = "The Verge",
            rssUrl = "https://www.theverge.com/rss/index.xml",
            siteUrl = "https://www.theverge.com/",
            colorHex = 0xFFB15CFF,
            lang = "en"
        ),
        NewsSource(
            id = "arstechnica",
            name = "Ars Technica",
            rssUrl = "https://feeds.arstechnica.com/arstechnica/index",
            siteUrl = "https://arstechnica.com/",
            colorHex = 0xFFFF4D6D,
            lang = "en"
        ),
        NewsSource(
            id = "wired",
            name = "Wired",
            rssUrl = "https://www.wired.com/feed/rss",
            siteUrl = "https://www.wired.com/",
            colorHex = 0xFFFFD166,
            lang = "en"
        ),
        NewsSource(
            id = "mittr",
            name = "MIT Tech Review",
            rssUrl = "https://www.technologyreview.com/feed/",
            siteUrl = "https://www.technologyreview.com/",
            colorHex = 0xFF63E6FF,
            lang = "en"
        ),
        NewsSource(
            id = "devto",
            name = "DEV.to",
            rssUrl = "https://dev.to/feed/",
            siteUrl = "https://dev.to/",
            colorHex = 0xFF9AA8C7,
            lang = "en"
        )
    )

    fun sourceById(id: String): NewsSource? = NEWS.firstOrNull { it.id == id }

    /** Порядок категорий каталога ресурсов. */
    val RESOURCE_CATEGORIES: List<String> = listOf(
        "Новости и медиа",
        "Обучение",
        "Документация",
        "Инструменты",
        "Искусственный интеллект",
        "Сообщества",
        "Карьера",
        "Практика"
    )

    val RESOURCES: List<ItResource> = listOf(
        // ---------- Новости и медиа ----------
        ItResource("r-habr", "Хабр", "Крупнейшее русскоязычное IT-сообщество: статьи, переводы, новости и Q&A.", "https://habr.com/", "Новости и медиа", "ru"),
        ItResource("r-3dnews", "3DNews", "Железо, гаджеты, софт и научные достижения — ежедневные новости.", "https://3dnews.ru/", "Новости и медиа", "ru"),
        ItResource("r-opennet", "OpenNET", "Новости open source, Linux и всего, что с ними связано.", "https://www.opennet.ru/", "Новости и медиа", "ru"),
        ItResource("r-hn", "Hacker News", "Главная англоязычная лента IT-новостей от Y Combinator.", "https://news.ycombinator.com/", "Новости и медиа", "en"),
        ItResource("r-techcrunch", "TechCrunch", "Стартапы, венчурные инвестиции и крупные технические релизы.", "https://techcrunch.com/", "Новости и медиа", "en"),
        ItResource("r-verge", "The Verge", "Технологии, гаджеты и цифровая культура — с отличными разборами.", "https://www.theverge.com/", "Новости и медиа", "en"),
        ItResource("r-ars", "Ars Technica", "Глубокие технические статьи о железе, софте и науке.", "https://arstechnica.com/", "Новости и медиа", "en"),
        ItResource("r-wired", "Wired", "Технологии и их влияние на общество, экономику и политику.", "https://www.wired.com/", "Новости и медиа", "en"),
        ItResource("r-mittr", "MIT Technology Review", "ИИ, биотех и передовые технологии от редакции MIT.", "https://www.technologyreview.com/", "Новости и медиа", "en"),
        ItResource("r-devto", "DEV.to", "Статьи разработчиков для разработчиков на множестве языков.", "https://dev.to/", "Новости и медиа", "en"),

        // ---------- Обучение ----------
        ItResource("r-stepik", "Stepik", "Открытые курсы по программированию, Python, алгоритмам и не только.", "https://stepik.org/", "Обучение", "ru"),
        ItResource("r-hexlet", "Хекслет", "Практические курсы по бэкенду, фронтенду и тестированию.", "https://ru.hexlet.io/", "Обучение", "ru"),
        ItResource("r-metanit", "Метанит", "Учебные материалы по Kotlin, C#, Python, Java и веб-разработке.", "https://metanit.com/", "Обучение", "ru"),
        ItResource("r-freecodecamp", "freeCodeCamp", "Бесплатная программа обучения веб-разработке с проектами и сертификатами.", "https://www.freecodecamp.org/", "Обучение", "en"),
        ItResource("r-codecademy", "Codecademy", "Интерактивные курсы с кодом прямо в браузере.", "https://www.codecademy.com/", "Обучение", "en"),
        ItResource("r-w3schools", "W3Schools", "Классические туториалы и справочник по HTML, CSS, JS, SQL.", "https://www.w3schools.com/", "Обучение", "en"),
        ItResource("r-coursera", "Coursera", "Курсы университетов и компаний, включая Computer Science.", "https://www.coursera.org/", "Обучение", "en"),
        ItResource("r-roadmap", "roadmap.sh", "Дорожные карты развития для разработчиков: что учить и в каком порядке.", "https://roadmap.sh/", "Обучение", "en"),

        // ---------- Документация ----------
        ItResource("r-mdn", "MDN Web Docs", "Настольная книга веб-разработчика: HTML, CSS, JS, Web API.", "https://developer.mozilla.org/", "Документация", "en"),
        ItResource("r-devdocs", "DevDocs", "Десятки официальных документаций в одном быстром интерфейсе.", "https://devdocs.io/", "Документация", "en"),
        ItResource("r-android", "Android Developers", "Официальная документация и гайды по разработке под Android.", "https://developer.android.com/", "Документация", "en"),
        ItResource("r-kotlin", "Kotlin", "Официальный сайт языка Kotlin: доки, туториалы, koans.", "https://kotlinlang.org/", "Документация", "en"),
        ItResource("r-python", "Python Docs", "Документация и учебник по Python.", "https://docs.python.org/", "Документация", "en"),
        ItResource("r-cppref", "cppreference", "Полный справочник по C и C++ со стандартами и примерами.", "https://en.cppreference.com/", "Документация", "en"),
        ItResource("r-postgres", "PostgreSQL", "Официальная документация СУБД PostgreSQL.", "https://www.postgresql.org/docs/", "Документация", "en"),
        ItResource("r-react", "React", "Официальная документация React с интерактивными примерами.", "https://react.dev/", "Документация", "en"),
        ItResource("r-mslearn", "Microsoft Learn", "Документация и обучение по C#, .NET, Azure и др.", "https://learn.microsoft.com/", "Документация", "en"),
        ItResource("r-go", "Go", "Документация языка Go и стандартной библиотеки.", "https://go.dev/doc/", "Документация", "en"),
        ItResource("r-rust", "Rust", "Книга по Rust и справочник стандартной библиотеки.", "https://www.rust-lang.org/learn", "Документация", "en"),

        // ---------- Инструменты ----------
        ItResource("r-github", "GitHub", "Главный хостинг кода: проекты, open source, Actions.", "https://github.com/", "Инструменты", "en"),
        ItResource("r-gitlab", "GitLab", "DevOps-платформа с CI/CD и приватными репозиториями.", "https://gitlab.com/", "Инструменты", "en"),
        ItResource("r-regex101", "regex101", "Конструктор, тестер и отладчик регулярных выражений.", "https://regex101.com/", "Инструменты", "en"),
        ItResource("r-caniuse", "Can I use", "Таблицы поддержки веб-фич во всех браузерах.", "https://caniuse.com/", "Инструменты", "en"),
        ItResource("r-codepen", "CodePen", "Песочница для фронтенда: HTML/CSS/JS-эксперименты и вдохновение.", "https://codepen.io/", "Инструменты", "en"),
        ItResource("r-jsfiddle", "JSFiddle", "Онлайн-песочница для JS, HTML и CSS с шарингом результата.", "https://jsfiddle.net/", "Инструменты", "en"),
        ItResource("r-excalidraw", "Excalidraw", "Рисование схем и архитектурных диаграмм «от руки».", "https://excalidraw.com/", "Инструменты", "en"),
        ItResource("r-jsoncrack", "JSON Crack", "Визуализация JSON/YAML/XML в граф — удобно для разбора структур.", "https://jsoncrack.com/", "Инструменты", "en"),
        ItResource("r-quicktype", "quicktype", "Генератор моделей/классов из JSON для десятков языков.", "https://app.quicktype.io/", "Инструменты", "en"),
        ItResource("r-tldr", "tldr pages", "Короткие шпаргалки по консольным командам с примерами.", "https://tldr.sh/", "Инструменты", "en"),
        ItResource("r-cyberchef", "CyberChef", "«Швейцарский нож»: кодировки, хэши, данные — всё в браузере.", "https://gchq.github.io/CyberChef/", "Инструменты", "en"),
        ItResource("r-figma", "Figma", "Совместный дизайн интерфейсов прямо в браузере.", "https://www.figma.com/", "Инструменты", "en"),

        // ---------- Искусственный интеллект ----------
        ItResource("r-huggingface", "Hugging Face", "Хаб моделей и датасетов для машинного обучения.", "https://huggingface.co/", "Искусственный интеллект", "en"),
        ItResource("r-paperscode", "Papers With Code", "Научные статьи по ML с привязанным кодом и метриками.", "https://paperswithcode.com/", "Искусственный интеллект", "en"),
        ItResource("r-arxiv", "arXiv (cs)", "Препринты научных статей по информатике и смежным областям.", "https://arxiv.org/list/cs/recent", "Искусственный интеллект", "en"),
        ItResource("r-chatgpt", "ChatGPT", "Диалоговый ИИ-помощник: код, объяснения, идеи.", "https://chatgpt.com/", "Искусственный интеллект", "en"),
        ItResource("r-aistudio", "Google AI Studio", "Эксперименты с моделями Gemini прямо в браузере.", "https://aistudio.google.com/", "Искусственный интеллект", "en"),
        ItResource("r-kaggle", "Kaggle", "Соревнования по ML, датасеты и ноутбуки сообщества.", "https://www.kaggle.com/", "Искусственный интеллект", "en"),

        // ---------- Сообщества ----------
        ItResource("r-stackoverflow", "Stack Overflow", "Крупнейшая база вопросов и ответов по программированию.", "https://stackoverflow.com/", "Сообщества", "en"),
        ItResource("r-reddit-prog", "r/programming", "Reddit-лента статей и дискуссий о разработке.", "https://www.reddit.com/r/programming/", "Сообщества", "en"),
        ItResource("r-lobsters", "Lobsters", "Ссылки и вдумчивые обсуждения вокруг технологий.", "https://lobste.rs/", "Сообщества", "en"),
        ItResource("r-hashnode", "Hashnode", "Блоги разработчиков на собственных доменах.", "https://hashnode.com/", "Сообщества", "en"),
        ItResource("r-lor", "Linux.org.ru", "Русскоязычное Linux-сообщество: новости, форумы, galactic.", "https://www.linux.org.ru/", "Сообщества", "ru"),
        ItResource("r-habr-qa", "Хабр Q&A", "Вопросы и ответы от сообщества Хабра.", "https://qna.habr.com/", "Сообщества", "ru"),

        // ---------- Карьера ----------
        ItResource("r-hh", "hh.ru", "Крупнейший в СНГ поиск вакансий, включая IT-раздел.", "https://hh.ru/", "Карьера", "ru"),
        ItResource("r-habrcareer", "Хабр Карьера", "IT-вакансии, зарплатные рейтинги и тесты навыков.", "https://career.habr.com/", "Карьера", "ru"),
        ItResource("r-linkedin", "LinkedIn", "Профессиональная сеть: связи, вакансии, подписки.", "https://www.linkedin.com/", "Карьера", "en"),
        ItResource("r-glassdoor", "Glassdoor", "Отзывы о работодателях и зарплаты от инсайдеров.", "https://www.glassdoor.com/", "Карьера", "en"),
        ItResource("r-welovedevs", "WeLoveNoCode / Job Boards", "Подборка досок объявлений для удалённой IT-работы.", "https://weworkremotely.com/", "Карьера", "en"),

        // ---------- Практика ----------
        ItResource("r-leetcode", "LeetCode", "Задачи с технических собеседований и подготовка к ним.", "https://leetcode.com/", "Практика", "en"),
        ItResource("r-codewars", "Codewars", "Ката по программированию с рейтингом и сообществом.", "https://www.codewars.com/", "Практика", "en"),
        ItResource("r-exercism", "Exercism", "Практика на 70+ языках с бесплатными менторами.", "https://exercism.org/", "Практика", "en"),
        ItResource("r-overthewire", "OverTheWire", "Wargames для изучения Linux и безопасности.", "https://overthewire.org/wargames/", "Практика", "en"),
        ItResource("r-frontendmentor", "Frontend Mentor", "Реальные макеты для тренировки вёрстки и фронтенда.", "https://www.frontendmentor.io/", "Практика", "en")
    )
}
