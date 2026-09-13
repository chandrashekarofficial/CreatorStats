/* =========================================================
   CreatorStats Ã¢â‚¬â€ Landing Page JavaScript
   ========================================================= */

"use strict";


/* =========================================================
   HELPERS
   ========================================================= */

const $ = (id) => document.getElementById(id);

function num(value) {
    const n = Number(value || 0);

    if (n >= 1e9) {
        return (n / 1e9).toFixed(2).replace(/\.00$/, "") + "B";
    }

    if (n >= 1e6) {
        return (n / 1e6).toFixed(2).replace(/\.00$/, "") + "M";
    }

    if (n >= 1e3) {
        return (n / 1e3).toFixed(2).replace(/\.00$/, "") + "K";
    }

    return n.toLocaleString();
}

function esc(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function date(value) {
    if (!value) return "Ã¢â‚¬â€";

    const d = new Date(value);

    if (Number.isNaN(d.getTime())) {
        return "Ã¢â‚¬â€";
    }

    return d.toLocaleDateString(undefined, {
        day: "numeric",
        month: "short",
        year: "numeric"
    });
}

function duration(seconds) {
    const sec = Number(seconds || 0);

    if (!sec) return "Ã¢â‚¬â€";

    const minutes = Math.floor(sec / 60);
    const remaining = sec % 60;

    return `${minutes}:${String(remaining).padStart(2, "0")}`;
}


/* =========================================================
   STATE
   ========================================================= */

let currentChannel = null;


/* =========================================================
   ANALYZE CHANNEL
   ========================================================= */

async function analyze(forcedQuery = null) {

    const input = $("query");
    const error = $("err");
    const button = $("go");

    if (!input || !button) {
        console.error("CreatorStats: analyzer elements not found.");
        return;
    }

    const query = String(
        forcedQuery !== null
            ? forcedQuery
            : input.value
    ).trim();

    error.textContent = "";

    if (!query) {
        error.textContent =
            "Enter a YouTube channel URL, @handle, or channel ID.";

        input.focus();
        return;
    }

    input.value = query;

    button.disabled = true;

    const originalButtonHTML = button.innerHTML;

    button.innerHTML = `
        <span class="loading-spinner"></span>
        Analyzing...
    `;

    try {

        const response = await fetch(
            "/api/public/youtube/channel?query=" +
            encodeURIComponent(query)
        );

        let data = {};

        try {
            data = await response.json();
        } catch {
            throw new Error(
                "The server returned an invalid response."
            );
        }

        if (!response.ok) {
            throw new Error(
                data.message ||
                data.error ||
                "Unable to analyze this YouTube channel."
            );
        }

        currentChannel = data;

        window.location.href = "/analytics.html?channel=" + encodeURIComponent(data.channelId || query);

    } catch (errorObject) {

        console.error(
            "CreatorStats analyzer error:",
            errorObject
        );

        error.textContent =
            errorObject.message ||
            "Something went wrong while analyzing the channel.";

    } finally {

        button.disabled = false;

        button.innerHTML = originalButtonHTML || `
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="11" cy="11" r="6.5"></circle>
                <path d="m16 16 5 5"></path>
            </svg>
            Analyze
        `;
    }
}


/* =========================================================
   RENDER CHANNEL RESULTS
   ========================================================= */

function renderChannel(data) {

    const dash = $("dash");

    if (!dash) {
        console.error(
            "CreatorStats: #dash was not found in index.html."
        );
        return;
    }

    dash.classList.remove("hide");

    const videos = Array.isArray(data.recentVideos)
        ? data.recentVideos
        : [];

    const totalVideos = Number(data.videos || 0);
    const totalViews = Number(data.views || 0);

    const averageViews =
        totalVideos > 0
            ? Math.round(totalViews / totalVideos)
            : 0;


    /* -----------------------------------------------------
       Channel information
    ----------------------------------------------------- */

    if ($("name")) {
        $("name").textContent =
            data.title || "YouTube Channel";
    }

    if ($("meta")) {
        $("meta").textContent =
            `${num(data.subscribers)} subscribers Ã‚Â· ${num(data.videos)} videos`;
    }

    if ($("avatar")) {
        $("avatar").src = data.thumbnail || "";
        $("avatar").alt =
            `${data.title || "Channel"} avatar`;
    }


    /* -----------------------------------------------------
       Metrics
    ----------------------------------------------------- */

    if ($("subs")) {
        $("subs").textContent =
            data.hiddenSubscribers
                ? "Hidden"
                : num(data.subscribers);
    }

    if ($("views")) {
        $("views").textContent =
            num(data.views);
    }

    if ($("videos")) {
        $("videos").textContent =
            num(data.videos);
    }

    if ($("avg")) {
        $("avg").textContent =
            averageViews
                ? num(averageViews)
                : "Ã¢â‚¬â€";
    }


    /* -----------------------------------------------------
       Channel snapshot
    ----------------------------------------------------- */

    if ($("created")) {
        $("created").textContent =
            date(data.publishedAt);
    }

    if ($("country")) {
        $("country").textContent =
            data.country || "Not public";
    }

    if ($("cid")) {
        $("cid").textContent =
            data.channelId || "Ã¢â‚¬â€";
    }


    /* -----------------------------------------------------
       YouTube link
    ----------------------------------------------------- */

    if ($("yt") && data.channelId) {

        $("yt").href =
            "https://www.youtube.com/channel/" +
            encodeURIComponent(data.channelId);

        $("yt").target = "_blank";
        $("yt").rel = "noopener noreferrer";
    }


    /* -----------------------------------------------------
       Recent performance bars
    ----------------------------------------------------- */

    renderPerformanceBars(videos);


    /* -----------------------------------------------------
       Recent video lists
    ----------------------------------------------------- */

    const sortedVideos = [...videos].sort(
        (a, b) =>
            Number(b.views || 0) -
            Number(a.views || 0)
    );

    const shorts = videos.filter(
        video =>
            Number(video.durationSeconds || 0) <= 60
    );

    const longVideos = videos.filter(
        video =>
            Number(video.durationSeconds || 0) > 60
    );


    if ($("overviewList")) {
        $("overviewList").innerHTML =
            videoListHTML(sortedVideos);
    }

    if ($("videosList")) {
        $("videosList").innerHTML =
            longVideos.length
                ? videoListHTML(longVideos)
                : emptyMessage(
                    "No long-form videos detected."
                );
    }

    if ($("shortsList")) {
        $("shortsList").innerHTML =
            shorts.length
                ? videoListHTML(shorts)
                : emptyMessage(
                    "No Shorts detected in the latest uploads."
                );
    }


    /* -----------------------------------------------------
       Analytics
    ----------------------------------------------------- */

    const totalRecent = videos.length || 1;

    const shortPercentage =
        Math.round(
            (shorts.length / totalRecent) * 100
        );

    const videoPercentage =
        Math.round(
            (longVideos.length / totalRecent) * 100
        );

    const likes = videos.reduce(
        (sum, video) =>
            sum + Number(video.likes || 0),
        0
    );

    const comments = videos.reduce(
        (sum, video) =>
            sum + Number(video.comments || 0),
        0
    );

    const recentViews = videos.reduce(
        (sum, video) =>
            sum + Number(video.views || 0),
        0
    );

    const engagement =
        recentViews > 0
            ? Math.round(
                ((likes + comments) /
                    recentViews) *
                10000
            ) / 100
            : 0;


    if ($("mix")) {

        $("mix").innerHTML = `
            <strong>
                ${shortPercentage}% Shorts Ã‚Â·
                ${videoPercentage}% Videos
            </strong>

            <p>
                ${videos.length}
                recent uploads analyzed by duration.
            </p>
        `;
    }


    if ($("engagement")) {

        $("engagement").innerHTML = `
            <strong>${engagement}%</strong>

            <p>
                Likes and comments relative to
                recent public views.
            </p>
        `;
    }


    /* -----------------------------------------------------
       Milestones
    ----------------------------------------------------- */

    renderMilestones(data);


    /* -----------------------------------------------------
       Future scenarios
    ----------------------------------------------------- */

    renderFuture(data);


    /* -----------------------------------------------------
       Scroll to results
    ----------------------------------------------------- */

    setTimeout(() => {

        dash.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });

    }, 100);
}


/* =========================================================
   PERFORMANCE BARS
   ========================================================= */

function renderPerformanceBars(videos) {

    const container = $("bars");

    if (!container) return;

    if (!videos.length) {

        container.innerHTML = `
            <div class="analytics-box">
                No recent video data available.
            </div>
        `;

        return;
    }

    const values = videos.map(
        video => Number(video.views || 0)
    );

    const max =
        Math.max(...values, 1);

    container.innerHTML =
        [...videos]
            .reverse()
            .map(video => {

                const views =
                    Number(video.views || 0);

                const height =
                    Math.max(
                        8,
                        (views / max) * 92
                    );

                return `
                    <div
                        class="bar"
                        style="height:${height}%"
                        title="${esc(video.title || "Video")}: ${num(views)} views"
                    >
                        <span>${num(views)}</span>
                    </div>
                `;
            })
            .join("");
}


/* =========================================================
   VIDEO LIST
   ========================================================= */

function videoListHTML(items) {

    if (!items.length) {

        return emptyMessage(
            "No recent videos available."
        );
    }

    return items
        .map(video => {

            const seconds =
                Number(video.durationSeconds || 0);

            const type =
                video.type ||
                (seconds <= 60
                    ? "Short"
                    : "Video");

            return `
                <div class="video-row">

                    <img
                        src="${esc(video.thumbnail || "")}"
                        alt=""
                        loading="lazy"
                    >

                    <div>
                        <div class="video-title">
                            ${esc(video.title || "Untitled video")}
                        </div>

                        <div class="video-sub">
                            ${esc(type)}
                            Ã‚Â·
                            ${duration(seconds)}
                            Ã‚Â·
                            ${date(video.publishedAt)}
                        </div>
                    </div>

                    <div class="video-stats">
                        ${num(video.views)}
                        <small>views</small>
                    </div>

                </div>
            `;
        })
        .join("");
}


/* =========================================================
   EMPTY MESSAGE
   ========================================================= */

function emptyMessage(message) {

    return `
        <div class="analytics-box">
            ${esc(message)}
        </div>
    `;
}


/* =========================================================
   MILESTONES
   ========================================================= */

function renderMilestones(data) {

    const container = $("milestones");

    if (!container) return;

    const subscribers =
        Number(data.subscribers || 0);

    const views =
        Number(data.views || 0);

    const videos =
        Number(data.videos || 0);


    const subscriberTargets = [
        1000,
        10000,
        100000,
        1000000,
        10000000,
        100000000
    ];

    const viewTargets = [
        1000000,
        10000000,
        100000000,
        1000000000,
        10000000000,
        100000000000
    ];


    const nextSubscriber =
        subscriberTargets.find(
            target => target > subscribers
        );

    const nextView =
        viewTargets.find(
            target => target > views
        );


    container.innerHTML = `

        <div class="milestone">

            <span>
                Current subscribers
            </span>

            <strong>
                ${
                    data.hiddenSubscribers
                        ? "Hidden"
                        : num(subscribers)
                }
            </strong>

            <span>
                ${
                    nextSubscriber
                        ? num(nextSubscriber) +
                          " next milestone"
                        : "Major milestone range reached"
                }
            </span>

        </div>


        <div class="milestone">

            <span>
                Current views
            </span>

            <strong>
                ${num(views)}
            </strong>

            <span>
                ${
                    nextView
                        ? num(nextView) +
                          " next view milestone"
                        : "Major milestone range reached"
                }
            </span>

        </div>


        <div class="milestone">

            <span>
                Published videos
            </span>

            <strong>
                ${num(videos)}
            </strong>

            <span>
                Public lifetime count
            </span>

        </div>
    `;
}


/* =========================================================
   FUTURE SCENARIOS
   ========================================================= */

function renderFuture(data) {

    const container = $("future");

    if (!container) return;

    const subscribers =
        Number(data.subscribers || 0);

    const views =
        Number(data.views || 0);

    const videos =
        Number(data.videos || 0);

    const averageViews =
        videos > 0
            ? views / videos
            : 0;


    const scenarios = [

        [
            "1% subscriber growth",
            num(Math.round(subscribers * 1.01)),
            "scenario"
        ],

        [
            "5% subscriber growth",
            num(Math.round(subscribers * 1.05)),
            "scenario"
        ],

        [
            "10% subscriber growth",
            num(Math.round(subscribers * 1.10)),
            "scenario"
        ],

        [
            "100 more videos",
            num(
                Math.round(
                    views +
                    averageViews * 100
                )
            ),
            "if average holds"
        ],

        [
            "500 more videos",
            num(
                Math.round(
                    views +
                    averageViews * 500
                )
            ),
            "if average holds"
        ],

        [
            "1,000 more videos",
            num(
                Math.round(
                    views +
                    averageViews * 1000
                )
            ),
            "if average holds"
        ]
    ];


    container.innerHTML =
        scenarios
            .map(
                ([title, value, note]) => `
                    <div class="future-card">

                        <span>
                            ${esc(title)}
                        </span>

                        <strong>
                            ${value}
                        </strong>

                        <span>
                            ${esc(note)}
                        </span>

                    </div>
                `
            )
            .join("");
}


/* =========================================================
   VIEW SWITCHING
   ========================================================= */

function showView(view) {

    document
        .querySelectorAll(".view")
        .forEach(element => {
            element.classList.add("hidden");
        });


    const target =
        $("view-" + view);

    if (target) {
        target.classList.remove("hidden");
    }


    document
        .querySelectorAll(".nav-item")
        .forEach(item => {

            item.classList.toggle(
                "active",
                item.dataset.view === view
            );

        });
}


/* =========================================================
   FOCUS SEARCH
   ========================================================= */

function focusSearch() {

    const input = $("query");

    if (!input) return;

    input.focus();

    input.scrollIntoView({
        behavior: "smooth",
        block: "center"
    });
}


/* =========================================================
   INITIALIZE
   ========================================================= */

const analyticsChannel = new URLSearchParams(window.location.search).get("channel");

if (analyticsChannel && window.location.pathname.endsWith("/analytics.html")) {
    fetch("/api/public/youtube/channel?query=" + encodeURIComponent(analyticsChannel))
        .then(response => response.json().then(data => ({ ok: response.ok, data })))
        .then(result => {
            if (!result.ok) throw new Error(result.data.message || result.data.error || "Unable to load channel.");
            currentChannel = result.data;
            renderChannel(result.data);
        })
        .catch(error => console.error("CreatorStats analytics error:", error));
}
document.addEventListener(
    "DOMContentLoaded",
    () => {

        const analyzeButton = $("go");

        const queryInput = $("query");

        const focusButton = $("focusSearch");

        const startButton = $("startSearch");


        /* ---------------------------------------------
           Analyze button
        --------------------------------------------- */

        if (analyzeButton) {

            analyzeButton.addEventListener(
                "click",
                () => analyze()
            );
        }


        /* ---------------------------------------------
           Enter key
        --------------------------------------------- */

        if (queryInput) {

            queryInput.addEventListener(
                "keydown",
                event => {

                    if (event.key === "Enter") {

                        event.preventDefault();

                        analyze();
                    }
                }
            );
        }


        /* ---------------------------------------------
           Search buttons
        --------------------------------------------- */

        if (focusButton) {

            focusButton.addEventListener(
                "click",
                focusSearch
            );
        }


        if (startButton) {

            startButton.addEventListener(
                "click",
                focusSearch
            );
        }


        /* ---------------------------------------------
           Dashboard navigation
        --------------------------------------------- */

        document
            .querySelectorAll(".nav-item")
            .forEach(item => {

                item.addEventListener(
                    "click",
                    () => {

                        if (!currentChannel) {

                            focusSearch();

                            return;
                        }

                        showView(
                            item.dataset.view
                        );
                    }
                );
            });


        /* ---------------------------------------------
           Ctrl + K
        --------------------------------------------- */

        document.addEventListener(
            "keydown",
            event => {

                if (
                    (event.ctrlKey || event.metaKey) &&
                    event.key.toLowerCase() === "k"
                ) {

                    event.preventDefault();

                    focusSearch();
                }
            }
        );


        /* ---------------------------------------------
           Popular channel buttons
        --------------------------------------------- */

        document
            .querySelectorAll(
                ".view-analytics, .view-analytics-btn, .popular-analyze, .view-analytics"
            )
            .forEach(button => {

                button.addEventListener(
                    "click",
                    () => {

                        const query =
                            button.dataset.query;

                        if (query) {
                            analyze(query);
                        } else {
                            focusSearch();
                        }
                    }
                );
            });


        /* ---------------------------------------------
           Generic data-query buttons
        --------------------------------------------- */

        document
            .querySelectorAll("[data-query]")
            .forEach(button => {

                if (
                    button.id === "query" ||
                    button.id === "go"
                ) {
                    return;
                }

                button.addEventListener(
                    "click",
                    () => {

                        const query =
                            button.dataset.query;

                        if (query) {
                            analyze(query);
                        }
                    }
                );
            });

        /* ---------------------------------------------
           Dynamic Popular Channels
        --------------------------------------------- */

        async function loadPopularChannels() {

            const container =
                document.getElementById("popularChannels");

            if (!container) {
                return;
            }

            try {

                const response =
                    await fetch("/api/public/youtube/popular");

                if (!response.ok) {
                    throw new Error(
                        "Popular channels request failed: " +
                        response.status
                    );
                }

                const channels =
                    await response.json();

                if (!Array.isArray(channels) || channels.length === 0) {
                    throw new Error(
                        "No popular channels returned."
                    );
                }

                const escapeHtml = value =>
                    String(value ?? "")
                        .replace(/&/g, "&amp;")
                        .replace(/</g, "&lt;")
                        .replace(/>/g, "&gt;")
                        .replace(/"/g, "&quot;")
                        .replace(/'/g, "&#039;");

                const formatNumber = value => {

                    const number = Number(value || 0);

                    if (number >= 1000000000) {
                        return (
                            (number / 1000000000)
                                .toFixed(1)
                                .replace(/\.0$/, "") +
                            "B"
                        );
                    }

                    if (number >= 1000000) {
                        return (
                            (number / 1000000)
                                .toFixed(1)
                                .replace(/\.0$/, "") +
                            "M"
                        );
                    }

                    if (number >= 1000) {
                        return (
                            (number / 1000)
                                .toFixed(1)
                                .replace(/\.0$/, "") +
                            "K"
                        );
                    }

                    return number.toLocaleString();
                };

                container.innerHTML =
                    channels
                        .slice(0, 5)
                        .map(channel => {

                            const title =
                                escapeHtml(channel.title);

                            const handle =
                                escapeHtml(channel.handle);

                            const thumbnail =
                                escapeHtml(channel.thumbnail);

                            const channelId =
                                escapeHtml(channel.channelId);

                            return `
                                <article
                                    class="channel-card"
                                    data-query="${handle}"
                                    data-channel-id="${channelId}"
                                >

                                    <div class="channel-avatar">
                                        <img
                                            src="${thumbnail}"
                                            alt="${title} channel logo"
                                            loading="lazy"
                                        >
                                    </div>

                                    <div class="channel-info">

                                        <h3>${title}</h3>

                                        <p class="channel-handle">
                                            ${handle}
                                        </p>

                                        <div class="channel-stats">

                                            <span>
                                                ${formatNumber(channel.subscribers)}
                                                subscribers
                                            </span>

                                            <span>
                                                ${formatNumber(channel.views)}
                                                views
                                            </span>

                                        </div>

                                    </div>

                                </article>
                            `;

                        })
                        .join("");

                container
                    .querySelectorAll(".channel-card")
                    .forEach(card => {

                        card.addEventListener(
                            "click",
                            () => {

                                const query =
                                    card.dataset.query;

                                if (
                                    query &&
                                    typeof analyze === "function"
                                ) {
                                    analyze(query);
                                }

                            }
                        );

                    });

            } catch (error) {

                console.error(
                    "Popular channels failed:",
                    error
                );

                container.innerHTML = `
                    <div class="popular-loading">
                        Unable to load popular channels.
                    </div>
                `;
            }
        }

        loadPopularChannels();

        console.log(
            "CreatorStats frontend initialized."
        );
    }
);




/* Popular Channels - View All navigation */
document.addEventListener("DOMContentLoaded", function () {
    const viewAllButton = document.querySelector(".view-all-btn");

    if (viewAllButton) {
        viewAllButton.addEventListener("click", function () {
            window.location.href = "/popular.html";
        });
    }
});




function toggleLandingProfile(){
    document.getElementById("profileMenu")?.classList.toggle("open");
}

function toggleLandingTheme(){
    const dark=document.documentElement.classList.contains("dark");
    localStorage.setItem("creatorstats_theme",dark?"light":"dark");
    document.documentElement.classList.toggle("dark",!dark);
    const b=document.getElementById("themeMenuButton");
    if(b)b.textContent=!dark?"Light mode":"Dark mode";
}

function landingLogout(){
    ["creatorstats_token","creatorhub_token","token","jwt","accessToken","authToken","user","currentUser"]
        .forEach(k=>localStorage.removeItem(k));
    sessionStorage.clear();
    location.href="/";
}

document.addEventListener("click",function(e){
    if(!e.target.closest(".profile-menu")&&!e.target.closest("#profileButton")){
        document.getElementById("profileMenu")?.classList.remove("open");
    }
});


/* Landing profile menu */
(function () {
    function initLandingProfile() {
        const button = document.getElementById("profileButton");
        const menu = document.getElementById("profileMenu");

        if (!button || !menu || button.dataset.ready === "1") return;

        const hasToken =
            !!localStorage.getItem("creatorstats_token") ||
            !!localStorage.getItem("creatorhub_token") ||
            !!localStorage.getItem("token") ||
            !!localStorage.getItem("jwt") ||
            !!localStorage.getItem("accessToken") ||
            !!localStorage.getItem("authToken");

        if (!hasToken) {
            button.innerHTML = "<span>U</span> Sign In";
            button.onclick = function () {
                window.location.href = "/login.html";
            };
            return;
        }

        button.innerHTML = "<span>U</span> Account⌄";

        button.dataset.ready = "1";

        button.addEventListener("click", function (e) {
            e.stopPropagation();
            menu.classList.toggle("open");
        });

        document.addEventListener("click", function (e) {
            if (!e.target.closest(".profile-wrap")) {
                menu.classList.remove("open");
            }
        });

        const themeButton = document.getElementById("landingThemeButton");
        if (themeButton) {
            themeButton.addEventListener("click", function () {
                document.body.classList.toggle("dark");

                const dark = document.body.classList.contains("dark");
                localStorage.setItem("creatorstats_theme", dark ? "dark" : "light");

                themeButton.textContent = dark ? "Light mode" : "Dark mode";
            });
        }

        const logout = document.getElementById("landingLogout");
        if (logout) {
            logout.addEventListener("click", function () {
                [
                    "creatorstats_token",
                    "creatorhub_token",
                    "token",
                    "jwt",
                    "accessToken",
                    "authToken",
                    "user",
                    "currentUser"
                ].forEach(function (key) {
                    localStorage.removeItem(key);
                });

                sessionStorage.clear();
                window.location.href = "/";
            });
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initLandingProfile);
    } else {
        initLandingProfile();
    }
})();
