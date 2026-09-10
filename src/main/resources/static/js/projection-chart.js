"use strict";
(function(){
  const qs=new URLSearchParams(location.search),query=qs.get("channel")||"crzieo";
  let channel=null,history=[],projection=[];
  const rangeDays=1095;
  let windowDays=53,windowStart=0;
  const $=id=>document.getElementById(id);
  const esc=v=>String(v??"").replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;");
  const fmt=n=>{n=Number(n||0);if(n>=1e12)return(n/1e12).toFixed(2).replace(/\.00$/,'')+"T";if(n>=1e9)return(n/1e9).toFixed(2).replace(/\.00$/,'')+"B";if(n>=1e6)return(n/1e6).toFixed(2).replace(/\.00$/,'')+"M";if(n>=1e3)return(n/1e3).toFixed(2).replace(/\.00$/,'')+"K";return Math.round(n).toLocaleString()};
  const full=n=>Math.round(Number(n||0)).toLocaleString();
  const date=v=>v?new Date(v).toLocaleDateString(undefined,{month:"short",day:"numeric",year:"numeric"}):"—";
  const shortDate=v=>v?new Date(v).toLocaleDateString(undefined,{year:"numeric",month:"short"}):"";
  const api=async url=>{const r=await fetch(url);const d=await r.json();if(!r.ok)throw new Error(d?.message||d?.error||"Request failed");return d};
  const style=document.createElement("style");
  style.textContent=`
  .projection-panel{background:#fff;border:1px solid #dbe8f7;border-radius:8px;box-shadow:0 2px 8px rgba(25,74,125,.08);padding:18px;margin-bottom:16px}
  .projection-panel h2{margin:0 0 14px;color:#182b45;font-size:18px}
  .projection-chart-wrap{position:relative;width:100%;height:390px;border:1px solid #edf1f5;border-radius:4px;overflow:hidden;background:#fff}
  .projection-svg{display:block;width:100%;height:100%;touch-action:none;cursor:crosshair}
  .projection-grid-line{stroke:#dfe4e8;stroke-width:1}
  .projection-axis-text{fill:#65707c;font-size:11px;font-family:Arial,sans-serif}
  .projection-year-text{fill:#66717d;font-size:11px;font-family:Arial,sans-serif;text-anchor:middle}
  .projection-line{fill:none;stroke:#c94b3f;stroke-width:2.5;vector-effect:non-scaling-stroke}
  .projection-area{fill:#c94b3f;opacity:.16}
  .projection-marker-line{stroke:#d65b50;stroke-width:1;stroke-dasharray:4 4;opacity:.8}
  .projection-marker-text{fill:#3d4650;font-size:10px;font-weight:700;font-family:Arial,sans-serif;writing-mode:vertical-rl;transform:rotate(180deg)}
  .projection-hover-line{stroke:#333;stroke-width:1;stroke-dasharray:3 3;opacity:.65}
  .projection-hover-dot{fill:#fff;stroke:#c94b3f;stroke-width:2}
  .projection-tooltip{position:absolute;display:none;min-width:150px;padding:9px 11px;border:1px solid #d8dde3;border-radius:6px;background:#fff;box-shadow:0 5px 18px rgba(0,0,0,.15);font:12px Arial,sans-serif;color:#202a34;pointer-events:none;z-index:5}
  .projection-tooltip strong{display:block;font-size:13px;margin-bottom:3px}
  .projection-nav{margin-top:7px;height:58px;position:relative;border-top:1px solid #c8cdd2;border-bottom:1px solid #c8cdd2;background:#f5f5f5;overflow:hidden}
  .projection-nav svg{width:100%;height:100%;display:block}
  .projection-nav-area{fill:#c94b3f;opacity:.18}
  .projection-nav-line{fill:none;stroke:#c94b3f;stroke-width:1.5}
  .projection-nav-window{position:absolute;top:0;height:100%;border-left:5px solid #999;border-right:5px solid #999;background:rgba(255,255,255,.18);cursor:grab;box-sizing:border-box}
  .projection-nav-window:active{cursor:grabbing}
  .projection-nav-window:before,.projection-nav-window:after{content:"";position:absolute;top:15px;width:3px;height:27px;background:#777;border-radius:2px}
  .projection-nav-window:before{left:-5px}.projection-nav-window:after{right:-5px}
  .projection-range-row{display:flex;align-items:center;gap:10px;margin-top:8px;color:#65707c;font-size:11px}
  .projection-range-row input{flex:1;accent-color:#888}
  .projection-empty{padding:60px 20px;text-align:center;color:#687789}
  .projection-note{margin:0 0 14px;color:#718094;font-size:12px}
  @media(max-width:700px){.projection-chart-wrap{height:300px}.projection-panel{padding:12px}.projection-panel h2{font-size:16px}}
  `;document.head.appendChild(style);

  function makeSvg(id,series,label){
    const el=$(id);if(!el)return;
    const W=Math.max(760,el.clientWidth||900),H=390,L=58,R=18,T=28,B=43,innerW=W-L-R,innerH=H-T-B;
    const start=Math.max(0,Math.min(windowStart,Math.max(0,series.length-windowDays))),end=Math.min(series.length,start+windowDays),visible=series.slice(start,end);
    if(!visible.length){el.innerHTML='<div class="projection-empty">No projection data available yet.</div>';return}
    const values=visible.map(x=>Number(x.value)||0),min=Math.max(0,Math.min(...values)),max=Math.max(...values),pad=(max-min||Math.max(1,max*.08))*.08,yMin=Math.max(0,min-pad),yMax=max+pad;
    const x=i=>L+(i/(Math.max(1,visible.length-1)))*innerW,y=v=>T+innerH-((v-yMin)/(yMax-yMin))*innerH;
    const path=visible.map((p,i)=>(i?"L":"M")+x(i).toFixed(1)+" "+y(p.value).toFixed(1)).join(" "),area=path+` L ${x(visible.length-1).toFixed(1)} ${T+innerH} L ${x(0).toFixed(1)} ${T+innerH} Z`;
    let svg=`<svg class="projection-svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none" aria-label="${esc(label)}"><g>`;
    for(let i=0;i<=5;i++){const v=yMin+(yMax-yMin)*(1-i/5),yy=y(v);svg+=`<line class="projection-grid-line" x1="${L}" y1="${yy}" x2="${W-R}" y2="${yy}"/><text class="projection-axis-text" x="${L-9}" y="${yy+4}" text-anchor="end">${esc(fmt(v))}</text>`}
    const years=[];visible.forEach(p=>{const yr=new Date(p.date).getFullYear();if(!years.includes(yr))years.push(yr)});years.forEach(yr=>{const idx=visible.findIndex(p=>new Date(p.date).getFullYear()===yr);if(idx>=0)svg+=`<text class="projection-year-text" x="${x(idx)}" y="${H-16}">${yr}</text>`});
    if(label.toLowerCase().includes("subscriber")){[10000,20000,30000,50000,75000,100000,150000,200000,500000,1000000].forEach(target=>{if(target>yMin&&target<yMax){const idx=visible.findIndex(p=>p.value>=target);if(idx>=0){const xx=x(idx);svg+=`<line class="projection-marker-line" x1="${xx}" y1="${T}" x2="${xx}" y2="${T+innerH}"/><text class="projection-marker-text" x="${xx+4}" y="${T+42}">${fmt(target)}</text>`}}})}
    svg+=`<path class="projection-area" d="${area}"/><path class="projection-line" d="${path}"/><line id="${id}-hover" class="projection-hover-line" x1="-10" y1="${T}" x2="-10" y2="${T+innerH}"/><circle id="${id}-dot" class="projection-hover-dot" cx="-10" cy="-10" r="4"/></g></svg>`;
    el.innerHTML=svg;
    const svgEl=el.querySelector("svg"),tip=$(id+"Tooltip"),hover=$(id+"-hover"),dot=$(id+"-dot");
    el.onpointermove=e=>{const rect=svgEl.getBoundingClientRect(),px=(e.clientX-rect.left)/rect.width*W,idx=Math.max(0,Math.min(visible.length-1,Math.round((px-L)/innerW*(visible.length-1)))),p=visible[idx];if(!p)return;const xx=x(idx),yy=y(p.value);hover.setAttribute("x1",xx);hover.setAttribute("x2",xx);dot.setAttribute("cx",xx);dot.setAttribute("cy",yy);if(tip){tip.style.display="block";tip.innerHTML=`<strong>${esc(full(p.value))}</strong><span>${esc(date(p.date))}</span>`;tip.style.left=Math.min(el.clientWidth-170,Math.max(8,(xx/W)*el.clientWidth+12))+"px";tip.style.top=Math.max(8,(yy/H)*el.clientHeight-12)+"px"}};
    el.onpointerleave=()=>{hover.setAttribute("x1",-10);hover.setAttribute("x2",-10);dot.setAttribute("cx",-10);dot.setAttribute("cy",-10);if(tip)tip.style.display="none"};
  }

  function renderNav(series){
    const box=$("projectionNavigator");if(!box||!series.length)return;
    const W=Math.max(760,box.clientWidth||900),H=58,vals=series.map(x=>Number(x.value)||0),min=Math.min(...vals),max=Math.max(...vals),den=max-min||1,xx=i=>i/(Math.max(1,series.length-1))*W,yy=v=>8+(H-16)-((v-min)/den)*(H-16),path=series.map((p,i)=>(i?"L":"M")+xx(i).toFixed(1)+" "+yy(p.value).toFixed(1)).join(" ");
    box.innerHTML=`<svg viewBox="0 0 ${W} ${H}" preserveAspectRatio="none"><path class="projection-nav-area" d="${path} L ${W} ${H} L 0 ${H} Z"/><path class="projection-nav-line" d="${path}"/></svg><div class="projection-nav-window" id="projectionNavWindow"></div>`;
    const win=$("projectionNavWindow"),maxStart=Math.max(0,series.length-windowDays),update=()=>{win.style.left=(maxStart?windowStart/maxStart*100:0)+"%";win.style.width=Math.min(100,windowDays/series.length*100)+"%"};update();let dragging=false,lastX=0;
    win.onpointerdown=e=>{dragging=true;lastX=e.clientX;win.setPointerCapture?.(e.pointerId)};
    win.onpointermove=e=>{if(!dragging||!maxStart)return;const dx=e.clientX-lastX;lastX=e.clientX;windowStart=Math.max(0,Math.min(maxStart,windowStart+Math.round(dx/box.clientWidth*series.length)));update();drawAll()};
    win.onpointerup=()=>dragging=false;win.onpointercancel=()=>dragging=false;
  }

  function drawAll(){
    const subs=history.map(x=>({date:x.date,value:Number(x.subscribers||0)})).concat(projection.map(x=>({date:x.date,value:Number(x.expectedSubscribers??x.subscribers??0)})));
    const views=history.map(x=>({date:x.date,value:Number(x.views||0)})).concat(projection.map(x=>({date:x.date,value:Number(x.expectedViews??x.views??0)})));
    makeSvg("projectionSubscribers",subs,"Projected Subscribers");makeSvg("projectionViews",views,"Projected Views");renderNav(subs);
    const range=$("projectionRange");if(range){range.max=Math.max(0,subs.length-windowDays);range.value=windowStart}
    const label=$("projectionRangeLabel");if(label&&subs.length)label.textContent=`${shortDate(subs[Math.min(windowStart,subs.length-1)].date)} — ${shortDate(subs[Math.min(windowStart+windowDays-1,subs.length-1)].date)}`;
  }

  async function load(){
    try{
      channel=await api("/api/public/youtube/channel?query="+encodeURIComponent(query));
      const data=await api("/api/public/youtube/history?channelId="+encodeURIComponent(channel.channelId)+"&days=365&projectionDays="+rangeDays);
      history=Array.isArray(data.history)?data.history:[];projection=Array.isArray(data.projection)?data.projection:[];
      if($("projectionTitle"))$("projectionTitle").textContent="Projected Subscribers for "+(channel.title||"Channel");
      if($("projectionViewsTitle"))$("projectionViewsTitle").textContent="Projected Views for "+(channel.title||"Channel");
      if($("projectionNote"))$("projectionNote").textContent=data.projectionNote||"Future projections are estimates based on stored historical data.";
      if(!projection.length){$("projectionCharts").innerHTML='<div class="projection-empty">Future projections will appear after CreatorStats has stored enough historical snapshots.</div>';return}
      windowStart=0;drawAll();
      const range=$("projectionRange");if(range)range.oninput=()=>{windowStart=Number(range.value||0);drawAll()};
      window.addEventListener("resize",drawAll);
    }catch(e){if($("projectionCharts"))$("projectionCharts").innerHTML=`<div class="projection-empty">${esc(e.message||"Unable to load projections.")}</div>`}
  }
  document.addEventListener("DOMContentLoaded",load);
})();
