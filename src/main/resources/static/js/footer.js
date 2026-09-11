(function(){
  "use strict";

  function loadFooter(){
    if(document.querySelector(".footer")) return;
    document.querySelectorAll("footer:not(.footer)").forEach(function(oldFooter){oldFooter.remove();});
    fetch("/footer.html", {cache:"no-store"})
      .then(function(response){if(!response.ok) throw new Error("Footer unavailable");return response.text();})
      .then(function(html){
        if(document.querySelector(".footer")) return;
        var wrapper=document.createElement("div");wrapper.innerHTML=html.trim();
        var footer=wrapper.querySelector(".footer");if(!footer) return;document.body.appendChild(footer);
      })
      .catch(function(error){console.warn("CreatorStats footer could not be loaded.",error);});
  }

  function getToken(){
    return localStorage.getItem("creatorstats_token") || localStorage.getItem("token") || localStorage.getItem("jwt") || localStorage.getItem("accessToken") || localStorage.getItem("authToken");
  }

  function addAuthMenu(){
    var actions=document.querySelector(".nav-actions");
    if(!actions || actions.querySelector(".creatorstats-profile") || !getToken()) return;
    var signIn=document.getElementById("signInBtn");if(signIn) signIn.style.display="none";
    var wrap=document.createElement("div");wrap.className="creatorstats-profile";
    wrap.innerHTML='<button type="button" class="creatorstats-profile-btn" aria-label="Open profile menu" aria-expanded="false">C</button><div class="creatorstats-profile-menu" role="menu"><div class="creatorstats-profile-head"><strong>CreatorStats</strong><span>Account</span></div><a href="/pro-tools.html" role="menuitem">⭐ Go to Pro Tools</a><a href="/favorites.html" role="menuitem">♡ Favorites</a><a href="/settings.html" role="menuitem">👤 Account</a><button type="button" class="creatorstats-theme-toggle" role="menuitem">☼ Light mode</button><a href="/contact.html?subject=Support" role="menuitem">❓ Help</a><a href="/contact.html?subject=Feedback" role="menuitem">💬 Give Feedback</a><button type="button" class="creatorstats-logout" role="menuitem">🚪 Log Out</button></div>';
    actions.insertBefore(wrap,actions.querySelector(".mobile-menu-btn") || null);
    var btn=wrap.querySelector(".creatorstats-profile-btn");var menu=wrap.querySelector(".creatorstats-profile-menu");
    btn.addEventListener("click",function(event){event.stopPropagation();var open=wrap.classList.toggle("open");btn.setAttribute("aria-expanded",String(open));});
    document.addEventListener("click",function(){wrap.classList.remove("open");btn.setAttribute("aria-expanded","false");});
    menu.addEventListener("click",function(event){event.stopPropagation();});
    wrap.querySelector(".creatorstats-logout").addEventListener("click",function(){
      ["token","jwt","accessToken","authToken","user","currentUser"].forEach(function(key){localStorage.removeItem(key);});
      sessionStorage.clear();window.location.href="/";
    });
    if(!document.getElementById("creatorstats-profile-style")){
      var style=document.createElement("style");style.id="creatorstats-profile-style";
      style.textContent='.creatorstats-profile{position:relative;display:inline-flex;align-items:center}.creatorstats-profile-btn{width:40px;height:40px;border:1px solid rgba(37,99,235,.2);border-radius:50%;background:#2563eb;color:#fff;font-weight:800;font-size:15px;cursor:pointer;box-shadow:0 6px 18px rgba(37,99,235,.2)}.creatorstats-profile-menu{position:absolute;right:0;top:calc(100% + 10px);width:210px;padding:8px;border:1px solid rgba(15,23,42,.08);border-radius:14px;background:#fff;box-shadow:0 18px 45px rgba(15,23,42,.15);display:none;z-index:9999}.creatorstats-profile.open .creatorstats-profile-menu{display:block}.creatorstats-profile-head{padding:10px 11px 9px;border-bottom:1px solid #eef2f7;margin-bottom:5px;display:flex;flex-direction:column;gap:2px}.creatorstats-profile-head strong{font-size:14px;color:#0f172a}.creatorstats-profile-head span{font-size:11px;color:#64748b}.creatorstats-profile-menu a,.creatorstats-profile-menu button{display:flex;align-items:center;gap:10px;width:100%;box-sizing:border-box;padding:11px 12px;border:0;border-radius:9px;background:transparent;color:#334155;text-decoration:none;text-align:left;font:inherit;font-size:13px;cursor:pointer}.creatorstats-profile-menu a:hover,.creatorstats-profile-menu button:hover{background:#f1f5f9;color:#2563eb}.creatorstats-logout{color:#dc2626}.creatorstats-logout:hover{color:#b91c1c;background:#fef2f2}@media(max-width:700px){.creatorstats-profile-menu{right:-8px}.creatorstats-profile-btn{width:38px;height:38px}}';
      document.head.appendChild(style);
    }
  }

  function init(){loadFooter();addAuthMenu();}
  if(document.readyState === "loading") document.addEventListener("DOMContentLoaded",init); else init();
})();



