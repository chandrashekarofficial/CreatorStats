(function(){
  "use strict";

  function loadFooter(){
    if(document.querySelector(".footer")) return;

    fetch("/footer.html", {cache:"no-store"})
      .then(function(response){
        if(!response.ok) throw new Error("Footer unavailable");
        return response.text();
      })
      .then(function(html){
        if(document.querySelector(".footer")) return;
        var wrapper=document.createElement("div");
        wrapper.innerHTML=html.trim();
        var footer=wrapper.querySelector(".footer");
        if(!footer) return;
        document.body.appendChild(footer);
      })
      .catch(function(error){
        console.warn("CreatorStats footer could not be loaded.",error);
      });
  }

  if(document.readyState === "loading"){
    document.addEventListener("DOMContentLoaded",loadFooter);
  }else{
    loadFooter();
  }
})();
