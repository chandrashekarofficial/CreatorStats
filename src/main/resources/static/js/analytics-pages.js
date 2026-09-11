"use strict";
const qs=new URLSearchParams(location.search),channelQuery=qs.get("channel")||"crzieo",$=id=>document.getElementById(id),token=()=>localStorage.getItem("creatorstats_token")||localStorage.getItem("token"),signedIn=()=>!!token();
const num=n=>{n=Number(n||0);if(n>=1e9)return(n/1e9).toFixed(2).replace(/\.00$/,'')+'B';if(n>=1e6)return(n/1e6).toFixed(2).replace(/\.00$/,'')+'M';if(n>=1e3)return(n/1e3).toFixed(2).replace(/\.00$/,'')+'K';return n.toLocaleString()},date=v=>v?new Date(v).toLocaleDateString(undefined,{day:'numeric',month:'short',year:'numeric'}):'—',esc=v=>String(v??'').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const api=async(url,options={})=>{const r=await fetch(url,options);let data=null;try{data=await r.json()}catch{}if(!r.ok)throw new Error(data?.message||data?.error||'Request failed');return data};
let channelData=null,favorites=[],history=[],historyResponse={};
function authHeaders(){return{Authorization:'Bearer '+token(),'Content-Type':'application/json'}}function login(){location.href='/login.html?returnTo='+encodeURIComponent(location.pathname+location.search)}function openFavorites(){if(!signedIn())return login();location.href='/favorites.html'}function toggleMenu(){$('profileMenu')?.classList.toggle('open')}function globalAnalyze(){const v=$('globalSearch')?.value.trim();if(v)location.href='/analytics.html?channel='+encodeURIComponent(v)}function go(page){location.href='/'+page+'?channel='+encodeURIComponent(channelData?.channelId||channelQuery)}
async function loadFavorites(){if(!signedIn())return;try{const data=await api('/api/favorites',{headers:{Authorization:'Bearer '+token()}});favorites=Array.isArray(data)?data:[]}catch(e){console.warn(e)}}
function updateFavoriteButton(){const b=$('favoriteButton');if(!b||!channelData)return;const saved=favorites.some(f=>String(f.channelId)===String(channelData.channelId));b.textContent=saved?'★ Remove Favorite':'♡ Add to Favorites';b.classList.toggle('saved',saved)}
async function toggleFavorite(){if(!signedIn())return login();if(!channelData?.channelId)return;const saved=favorites.some(f=>String(f.channelId)===String(channelData.channelId)),b=$('favoriteButton');if(b)b.disabled=true;try{if(saved){await api('/api/favorites/'+encodeURIComponent(channelData.channelId),{method:'DELETE',headers:{Authorization:'Bearer '+token()}});favorites=favorites.filter(f=>String(f.channelId)!==String(channelData.channelId))}else{const body=await api('/api/favorites',{method:'POST',headers:authHeaders(),body:JSON.stringify({channelId:channelData.channelId,title:channelData.title||'YouTube Channel',handle:channelData.handle||null,thumbnail:channelData.thumbnail||null,youtubeUrl:channelData.youtubeUrl||('https://www.youtube.com/channel/'+channelData.channelId)})});favorites.push(body)}updateFavoriteButton();renderSidebarFavorites()}catch(e){alert(e.message)}finally{if(b)b.disabled=false}}
function renderSidebarFavorites(){const box=$('sidebarFavorites'),count=$('favoriteCount'),guest=$('guestFavoriteCard');if(count)count.textContent=favorites.length+' / 5';if(!box)return;if(!signedIn()){box.innerHTML='';if(guest)guest.style.display='block';return}if(guest)guest.style.display='none';box.innerHTML=favorites.slice(0,5).map(f=>`<div class="side-favorite" role="button" tabindex="0" onclick="location.href='/analytics.html?channel=${encodeURIComponent(f.channelId||'')}'" onkeydown="if(event.key==='Enter'||event.key===' ')location.href='/analytics.html?channel=${encodeURIComponent(f.channelId||'')}'"><img src="${esc(f.thumbnail||'')}" alt=""><div><strong>${esc(f.title||'YouTube Channel')}</strong><span>${esc(f.handle||'')}</span></div></div>`).join('')}
function standardizeProfileMenu(){const m=$('profileMenu');if(!m)return;const c=encodeURIComponent(channelData?.channelId||channelQuery);m.innerHTML=`<a href="/analytics.html?channel=${c}" class="profile-item">Home</a><button onclick="openFavorites()" class="profile-item">Favorites</button><a href="/pro-tools.html" class="profile-item">Go to Pro Tools</a><a href="/settings.html" class="profile-item">Account</a><button type="button" onclick="toggleTheme()" id="themeMenuButton" class="profile-item">Dark mode</button><div class="profile-separator"></div><a href="/help.html" class="profile-item">Help</a><a href="/contact.html?subject=Feedback" class="profile-item">Give Feedback</a><div class="profile-separator"></div><button type="button" onclick="['creatorstats_token','token','jwt','accessToken','authToken','user','currentUser'].forEach(k=>localStorage.removeItem(k));sessionStorage.clear();location.href='/'" class="profile-item profile-logout">Log Out</button>`}function renderShell(){if(!channelData)return;standardizeProfileMenu();$('name').textContent=channelData.title||'YouTube Channel';$('handle').textContent=channelData.handle||'@channel';$('meta').textContent=num(channelData.subscribers)+' subscribers · '+num(channelData.views)+' views · '+num(channelData.videos)+' videos';$('avatar').src=channelData.thumbnail||'';$('createdInline').textContent=date(channelData.publishedAt);$('countryInline').textContent=channelData.country||'Not public';const ytLink=$('yt');if(ytLink){ytLink.href=channelData.youtubeUrl||('https://www.youtube.com/channel/'+channelData.channelId)};$('banner').src=channelData.banner||'';if(!channelData.banner)$('banner').style.display='none';updateFavoriteButton();renderSidebarFavorites();if($('profileButton'))$('profileButton').innerHTML='<span>U</span> '+(signedIn()?'Account⌄':'Sign In');if($('guestSignIn'))$('guestSignIn').style.display=signedIn()?'none':'inline-flex'}
function cleanSidebar(){document.querySelector(".side-nav")?.remove();document.querySelector(".side-divider")?.remove()}function setupNav(){cleanSidebar();const page=document.body.dataset.page;const active={overview:'analytics.html',videos:'video-analytics.html',projections:'projections.html',live:'live-subscribers.html',achievements:'achievements.html'}[page];document.querySelectorAll('[data-page-link]').forEach(a=>{a.href='/'+a.dataset.pageLink+'?channel='+encodeURIComponent(channelData?.channelId||channelQuery);if(a.dataset.pageLink===active)a.classList.add('active')})}
function renderOverview(){$('grade').textContent=channelData.grade||'—';$('subs').textContent=channelData.hiddenSubscribers?'Hidden':num(channelData.subscribers);$('views').textContent=num(channelData.views);$('videos').textContent=num(channelData.videos);$('avg').textContent=num(channelData.averageViews);$('country').textContent=channelData.country||'Not public';$('category').textContent=channelData.category||'—';const rows=history.slice(-7).reverse();$('dailyBody').innerHTML=rows.length?rows.map(r=>`<tr><td>${date(r.date)}</td><td>${num(r.subscribers)}</td><td>${num(r.views)}</td><td>${num(r.videos)}</td></tr>`).join(''):'<tr><td colspan="4">No historical snapshots yet.</td></tr>'}
function renderVideos(){const list=Array.isArray(channelData.recentVideos)?channelData.recentVideos:[],grid=$('videoGrid');if(!grid)return;grid.innerHTML=list.map(v=>`<article class="video-card"><div class="video-thumb"><img src="${esc(v.thumbnail||'')}" alt="" loading="lazy"><span class="video-duration">${v.durationSeconds?Math.floor(v.durationSeconds/60)+':'+String(v.durationSeconds%60).padStart(2,'0'):''}</span></div><div class="video-card-body"><div class="video-card-title">${esc(v.title||'Untitled video')}</div><span class="video-type-label">${v.isShort?'Short':'Video'}</span><div class="video-stats-line">◉ ${num(v.views)} &nbsp; ♡ ${num(v.likes)} &nbsp; ◌ ${num(v.comments)}</div><div class="video-date">${date(v.publishedAt)}</div></div></article>`).join('')||'<div class="empty-state">No public videos returned.</div>'}
function renderProjection(){
  const box=$('projectionChart');
  if(!box)return;

  const data=Array.isArray(historyResponse.projection)?historyResponse.projection:[];
  const historyData=Array.isArray(historyResponse.history)?historyResponse.history:[];

  if(!historyResponse.projectionAvailable||historyData.length<2||!data.length){
    box.innerHTML='<div class="chart-empty">Projection becomes available after CreatorStats has enough stored historical snapshots.</div>';
    return;
  }

  const analysis=historyResponse.growthAnalysis||{};
  const note=$('projectionNote');

  if(note&&historyResponse.projectionNote){
    note.textContent=historyResponse.projectionNote;
  }

  const summary=$('projectionSummary');

  if(summary){
    summary.innerHTML=`
      <div class="projection-summary-card">
        <span>Current subscribers</span>
        <strong>${formatProjectionNumber(analysis.currentSubscribers)}</strong>
        <small>${formatGrowth(analysis.monthlySubscriberGrowth)}/month</small>
      </div>
      <div class="projection-summary-card">
        <span>Current views</span>
        <strong>${formatProjectionNumber(analysis.currentViews)}</strong>
        <small>${formatGrowth(analysis.monthlyViewGrowth)}/month</small>
      </div>
      <div class="projection-summary-card">
        <span>Growth trend</span>
        <strong>${analysis.trend||'?'}</strong>
        <small>${analysis.snapshotCount||0} historical snapshots</small>
      </div>
      <div class="projection-summary-card">
        <span>Forecast confidence</span>
        <strong>${analysis.confidence||'Low'}</strong>
        <small>${analysis.confidenceScore||0}% confidence score</small>
      </div>`;
  }

  let metric='subscribers';
  let scenario='expected';
  let range=365;

  const draw=()=>{
    const isSubscribers=metric==='subscribers';
    const title=isSubscribers?'Subscribers':'Views';

    const current=isSubscribers
      ?analysis.currentSubscribers
      :analysis.currentViews;

    if($('chartMetricTitle'))$('chartMetricTitle').textContent=title;

    if($('chartMetricSubtitle')){
      $('chartMetricSubtitle').textContent=
        scenario==='expected'
          ?'Historical performance and expected forecast'
          :scenario.charAt(0).toUpperCase()+scenario.slice(1)+' growth scenario';
    }

    if($('chartCurrentValue')){
      $('chartCurrentValue').textContent=formatProjectionNumber(current);
    }

    const forecastKey=
      scenario==='conservative'
        ?(isSubscribers?'conservativeSubscribers':'conservativeViews')
        :scenario==='optimistic'
          ?(isSubscribers?'optimisticSubscribers':'optimisticViews')
          :(isSubscribers?'expectedSubscribers':'expectedViews');

    const conservativeKey=
      isSubscribers?'conservativeSubscribers':'conservativeViews';

    const optimisticKey=
      isSubscribers?'optimisticSubscribers':'optimisticViews';

    const actual=historyData.map(x=>({
      date:x.date,
      value:Number(isSubscribers?x.subscribers:x.views)||0
    }));

    const forecast=data
      .filter(x=>Number(x.daysFromNow)<=range)
      .map(x=>({
        date:x.date,
        value:Number(x[forecastKey])||0
      }));

    const conservative=data
      .filter(x=>Number(x.daysFromNow)<=range)
      .map(x=>({
        date:x.date,
        value:Number(x[conservativeKey])||0
      }));

    const optimistic=data
      .filter(x=>Number(x.daysFromNow)<=range)
      .map(x=>({
        date:x.date,
        value:Number(x[optimisticKey])||0
      }));

    const values=[
      ...actual.map(x=>x.value),
      ...forecast.map(x=>x.value),
      ...conservative.map(x=>x.value),
      ...optimistic.map(x=>x.value)
    ];

    if(!values.length)return;

    const minValue=Math.min(...values);
    const maxValue=Math.max(...values);
    const padding=Math.max((maxValue-minValue)*0.12,1);

    const yMin=Math.max(0,minValue-padding);
    const yMax=maxValue+padding;

    const width=1000;
    const height=390;
    const left=68;
    const right=20;
    const top=18;
    const bottom=44;

    const plotWidth=width-left-right;
    const plotHeight=height-top-bottom;

    const startTime=new Date(actual[0].date).getTime();

    const endTime=new Date(
      forecast.length
        ?forecast[forecast.length-1].date
        :actual[actual.length-1].date
    ).getTime();

    const xForDate=date=>{
      const time=new Date(date).getTime();

      return left+
        ((time-startTime)/Math.max(1,endTime-startTime))*plotWidth;
    };

    const yForValue=value=>{
      return top+
        (1-(value-yMin)/Math.max(1,yMax-yMin))*plotHeight;
    };

    const pathFor=items=>{
      return items.map((item,index)=>{
        const x=xForDate(item.date);
        const y=yForValue(item.value);

        return `${index===0?'M':'L'} ${x.toFixed(2)} ${y.toFixed(2)}`;
      }).join(' ');
    };

    let svg=
      `<svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none">`;

    for(let i=0;i<=5;i++){
      const ratio=i/5;
      const y=top+ratio*plotHeight;
      const value=yMax-(yMax-yMin)*ratio;

      svg+=`
        <line class="chart-grid-line"
          x1="${left}" y1="${y}"
          x2="${width-right}" y2="${y}"/>
        <text class="chart-axis-label"
          x="${left-8}" y="${y+3}"
          text-anchor="end">${formatProjectionNumber(value)}</text>`;
    }

    const labelCount=range===1095?6:5;

    for(let i=0;i<labelCount;i++){
      const ratio=i/(labelCount-1);
      const timestamp=startTime+(endTime-startTime)*ratio;
      const dateValue=new Date(timestamp);
      const x=left+plotWidth*ratio;

      svg+=`
        <text class="chart-date-label"
          x="${x}" y="${height-12}"
          text-anchor="middle">
          ${dateValue.toLocaleDateString(undefined,{month:'short',year:'2-digit'})}
        </text>`;
    }

    if(conservative.length){
      svg+=`<path class="chart-conservative-line" d="${pathFor(conservative)}"/>`;
    }

    if(optimistic.length){
      svg+=`<path class="chart-optimistic-line" d="${pathFor(optimistic)}"/>`;
    }

    svg+=`<path class="chart-actual-line" d="${pathFor(actual)}"/>`;

    if(forecast.length){
      svg+=`<path class="chart-forecast-line" d="${pathFor(forecast)}"/>`;
    }

    const step=Math.max(1,Math.floor(actual.length/35));

    for(let i=0;i<actual.length;i+=step){
      const item=actual[i];

      svg+=`
        <circle class="chart-actual-point"
          cx="${xForDate(item.date)}"
          cy="${yForValue(item.value)}"
          r="2.5"/>`;
    }

    svg+=`
      <line id="projectionHoverLine"
        class="chart-hover-line"
        x1="0" y1="${top}"
        x2="0" y2="${height-bottom}"
        style="display:none"/>

      <circle id="projectionHoverPoint"
        class="chart-hover-point"
        cx="0" cy="0" r="5"
        style="display:none"/>

      </svg>`;

    box.innerHTML=svg;

    const svgEl=box.querySelector('svg');
    const tooltip=$('chartTooltip');

    const hoverItems=[...actual,...forecast];

    svgEl.addEventListener('mousemove',event=>{
      const rect=svgEl.getBoundingClientRect();

      const svgX=
        ((event.clientX-rect.left)/rect.width)*width;

      const ratio=Math.max(
        0,
        Math.min(
          1,
          (svgX-left)/plotWidth
        )
      );

      const targetTime=
        startTime+
        (endTime-startTime)*ratio;

      let nearest=null;
      let nearestDistance=Infinity;

      hoverItems.forEach(item=>{
        const distance=Math.abs(
          new Date(item.date).getTime()-targetTime
        );

        if(distance<nearestDistance){
          nearestDistance=distance;
          nearest=item;
        }
      });

      if(!nearest)return;

      const x=xForDate(nearest.date);
      const y=yForValue(nearest.value);

      const hoverLine=box.querySelector('#projectionHoverLine');
      const hoverPoint=box.querySelector('#projectionHoverPoint');

      if(hoverLine){
        hoverLine.setAttribute('x1',x);
        hoverLine.setAttribute('x2',x);
        hoverLine.style.display='block';
      }

      if(hoverPoint){
        hoverPoint.setAttribute('cx',x);
        hoverPoint.setAttribute('cy',y);
        hoverPoint.style.display='block';
      }

      if(tooltip){
        tooltip.innerHTML=
          `<strong>${formatProjectionNumber(nearest.value)}</strong>`+
          `<span>${new Date(nearest.date).toLocaleDateString(undefined,{day:'numeric',month:'short',year:'numeric'})}</span>`;

        tooltip.style.display='block';

        const localX=event.clientX-rect.left;
        const localY=event.clientY-rect.top;

        tooltip.style.left=Math.min(
          Math.max(8,localX+12),
          Math.max(8,box.clientWidth-tooltip.offsetWidth-8)
        )+'px';

        tooltip.style.top=Math.max(
          8,
          Math.min(
            box.clientHeight-tooltip.offsetHeight-8,
            localY-12
          )
        )+'px';
      }
    });

    svgEl.addEventListener('mouseleave',()=>{
      if(tooltip)tooltip.style.display='none';

      const hoverLine=box.querySelector('#projectionHoverLine');
      const hoverPoint=box.querySelector('#projectionHoverPoint');

      if(hoverLine)hoverLine.style.display='none';
      if(hoverPoint)hoverPoint.style.display='none';
    });

    const growth=$('projectionGrowth');

    if(growth){
      const monthly=isSubscribers
        ?analysis.monthlySubscriberGrowth
        :analysis.monthlyViewGrowth;

      const monthlyPercent=isSubscribers
        ?analysis.monthlySubscriberGrowthPercent
        :analysis.monthlyViewGrowthPercent;

      const oneYear=isSubscribers
        ?analysis.estimatedSubscribers1Year
        :analysis.estimatedViews1Year;

      const threeYear=isSubscribers
        ?analysis.estimatedSubscribers3Years
        :analysis.estimatedViews3Years;

      const r2=isSubscribers
        ?analysis.subscriberR2
        :analysis.viewR2;

      growth.innerHTML=`
        <div class="projection-growth-card">
          <span>Average monthly growth</span>
          <strong>${formatGrowth(monthly)}</strong>
          <small>${formatPercent(monthlyPercent)} monthly trend</small>
        </div>

        <div class="projection-growth-card">
          <span>1 year estimate</span>
          <strong>${formatProjectionNumber(oneYear)}</strong>
          <small>Expected ${title.toLowerCase()}</small>
        </div>

        <div class="projection-growth-card">
          <span>3 year estimate</span>
          <strong>${formatProjectionNumber(threeYear)}</strong>
          <small>Expected ${title.toLowerCase()}</small>
        </div>

        <div class="projection-growth-card">
          <span>Trend confidence</span>
          <strong>${analysis.confidence||'Low'}</strong>
          <small>R? ${formatPercent(Number(r2||0)*100)}</small>
        </div>`;
    }
  };

  document.querySelectorAll('.projection-toggle-btn').forEach(button=>{
    button.onclick=()=>{
      document.querySelectorAll('.projection-toggle-btn')
        .forEach(b=>b.classList.remove('active'));

      button.classList.add('active');
      metric=button.dataset.metric||'subscribers';
      draw();
    };
  });

  document.querySelectorAll('.projection-scenario').forEach(button=>{
    button.onclick=()=>{
      document.querySelectorAll('.projection-scenario')
        .forEach(b=>b.classList.remove('active'));

      button.classList.add('active');
      scenario=button.dataset.scenario||'expected';
      draw();
    };
  });

  document.querySelectorAll('.projection-range-btn').forEach(button=>{
    button.onclick=()=>{
      document.querySelectorAll('.projection-range-btn')
        .forEach(b=>b.classList.remove('active'));

      button.classList.add('active');
      range=Number(button.dataset.range)||365;
      draw();
    };
  });

  draw();
}

function formatProjectionNumber(value){
  const n=Number(value);

  if(!Number.isFinite(n))return '?';

  const abs=Math.abs(n);

  if(abs>=1000000000){
    return (n/1000000000)
      .toFixed(2)
      .replace(/\.00$/,'')+'B';
  }

  if(abs>=1000000){
    return (n/1000000)
      .toFixed(2)
      .replace(/\.00$/,'')+'M';
  }

  if(abs>=1000){
    return (n/1000)
      .toFixed(2)
      .replace(/\.00$/,'')+'K';
  }

  return Math.round(n).toLocaleString();
}

function formatGrowth(value){
  const n=Number(value);

  if(!Number.isFinite(n))return '?';

  return (n>0?'+':'')+formatProjectionNumber(n);
}

function formatPercent(value){
  const n=Number(value);

  if(!Number.isFinite(n))return '?';

  return (n>0?'+':'')+n.toFixed(1)+'%';
}

function renderLive(){const n=$('liveSubs');if(n)n.textContent=channelData.hiddenSubscribers?'Hidden':num(channelData.subscribers);if($('liveTime'))$('liveTime').textContent='Fetched '+new Date().toLocaleTimeString()}
function renderAchievements(){const s=Number(channelData.subscribers||0),v=Number(channelData.views||0),targets=[[1000,'1K subscribers',s],[5000,'5K subscribers',s],[10000,'10K subscribers',s],[50000,'50K subscribers',s],[100000,'100K subscribers',s],[1000000,'1M subscribers',s],[1000000,'1M views',v],[10000000,'10M views',v]];$('achievementGrid').innerHTML=targets.map(x=>`<div class="achievement-card ${x[2]>=x[0]?'unlocked':''}"><div class="achievement-icon">★</div><strong>${x[1]}</strong><span>${x[2]>=x[0]?'Unlocked':'Locked'}</span></div>`).join('')}
async function load(){try{channelData=await api('/api/public/youtube/channel?query='+encodeURIComponent(channelQuery));await loadFavorites();historyResponse=await api('/api/public/youtube/history?channelId='+encodeURIComponent(channelData.channelId)+'&days=365&projectionDays=1095');history=Array.isArray(historyResponse.history)?historyResponse.history:[];renderShell();setupNav();const page=document.body.dataset.page;if(page==='overview')renderOverview();if(page==='videos')renderVideos();if(page==='projections')renderProjection();if(page==='live')renderLive();if(page==='achievements')renderAchievements();$('loading')?.classList.add('hidden-section');$('dashboard')?.classList.remove('hidden-section')}catch(e){$('loading').textContent=e.message||'Unable to load analytics.'}}
function applyTheme(){const dark=localStorage.getItem('creatorstats_theme')==='dark';document.documentElement.classList.toggle('dark',dark);const b=$("themeMenuButton");if(b)b.textContent=dark?'Light mode':'Dark mode'}function toggleTheme(){const dark=document.documentElement.classList.contains('dark');localStorage.setItem('creatorstats_theme',dark?'light':'dark');applyTheme()}applyTheme();window.toggleFavorite=toggleFavorite;window.openFavorites=openFavorites;window.toggleMenu=toggleMenu;window.globalAnalyze=globalAnalyze;window.go=go;window.login=login;window.refreshChannel=()=>location.reload();
document.addEventListener('click',e=>{if(!e.target.closest('.profile-menu')&&!e.target.closest('#profileButton'))$('profileMenu')?.classList.remove('open')});if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',load);else load();



