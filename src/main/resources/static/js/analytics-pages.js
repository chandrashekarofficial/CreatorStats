"use strict";

const qs = new URLSearchParams(location.search);
const channelQuery = qs.get("channel") || "crzieo";
const $ = id => document.getElementById(id);
const token = () => localStorage.getItem("creatorstats_token") || localStorage.getItem("token");
const signedIn = () => !!token();
const num = n => { n = Number(n || 0); if (n >= 1e9) return (n/1e9).toFixed(2).replace(/\.00$/,'')+'B'; if (n >= 1e6) return (n/1e6).toFixed(2).replace(/\.00$/,'')+'M'; if (n >= 1e3) return (n/1e3).toFixed(2).replace(/\.00$/,'')+'K'; return n.toLocaleString(); };
const date = v => v ? new Date(v).toLocaleDateString(undefined,{day:'numeric',month:'short',year:'numeric'}) : '—';
const esc = v => String(v ?? '').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const api = async (url, options={}) => { const r = await fetch(url, options); let data=null; try { data=await r.json(); } catch {} if(!r.ok) throw new Error(data?.message || data?.error || 'Request failed'); return data; };

let channelData = null;
let favorites = [];
let history = [];

function authHeaders(){ return {Authorization:'Bearer '+token(),'Content-Type':'application/json'}; }
function login(){ location.href='/login.html?returnTo='+encodeURIComponent(location.pathname+location.search); }
function openFavorites(){ if(!signedIn()) return login(); location.href='/favorites.html'; }
function toggleMenu(){ if(!signedIn()) return login(); $('profileMenu')?.classList.toggle('open'); }
function globalAnalyze(){ const v=$('globalSearch')?.value.trim(); if(v) location.href='/analytics.html?channel='+encodeURIComponent(v); }
function go(page){ location.href='/'+page+'?channel='+encodeURIComponent(channelData?.channelId || channelQuery); }

async function loadFavorites(){
  if(!signedIn()) return;
  try { const data=await api('/api/favorites',{headers:{Authorization:'Bearer '+token()}}); favorites=Array.isArray(data)?data:[]; } catch(e){ console.warn(e); }
}
function updateFavoriteButton(){
  const b=$('favoriteButton'); if(!b || !channelData) return;
  const saved=favorites.some(f=>String(f.channelId)===String(channelData.channelId));
  b.textContent=saved?'★ Remove Favorite':'♡ Add to Favorites'; b.classList.toggle('saved',saved);
}
async function toggleFavorite(){
  if(!signedIn()) return login(); if(!channelData?.channelId) return;
  const saved=favorites.some(f=>String(f.channelId)===String(channelData.channelId)); const b=$('favoriteButton'); if(b)b.disabled=true;
  try{
    if(saved){ await api('/api/favorites/'+encodeURIComponent(channelData.channelId),{method:'DELETE',headers:{Authorization:'Bearer '+token()}}); favorites=favorites.filter(f=>String(f.channelId)!==String(channelData.channelId)); }
    else { const body=await api('/api/favorites',{method:'POST',headers:authHeaders(),body:JSON.stringify({channelId:channelData.channelId,title:channelData.title||'YouTube Channel',handle:channelData.handle||null,thumbnail:channelData.thumbnail||null,youtubeUrl:channelData.youtubeUrl||('https://www.youtube.com/channel/'+channelData.channelId)})}); favorites.push(body); }
    updateFavoriteButton(); renderSidebarFavorites();
  }catch(e){ alert(e.message); } finally { if(b)b.disabled=false; }
}
function renderSidebarFavorites(){
  const box=$('sidebarFavorites'), count=$('favoriteCount'), guest=$('guestFavoriteCard'); if(count)count.textContent=favorites.length+' / 5'; if(!box)return;
  if(!signedIn()){box.innerHTML=''; if(guest)guest.style.display='block'; return;} if(guest)guest.style.display='none';
  box.innerHTML=favorites.slice(0,5).map(f=>`<div class="side-favorite"><img src="${esc(f.thumbnail||'')}" alt=""><div><strong>${esc(f.title||'YouTube Channel')}</strong><span>${esc(f.handle||'')}</span></div><button onclick="location.href='/analytics.html?channel=${encodeURIComponent(f.channelId||'')}'">⋮</button></div>`).join('');
}
function renderShell(){
  if(!channelData)return;
  $('name').textContent=channelData.title||'YouTube Channel'; $('handle').textContent=channelData.handle||'@channel';
  $('meta').textContent=num(channelData.subscribers)+' subscribers · '+num(channelData.views)+' views · '+num(channelData.videos)+' videos';
  $('avatar').src=channelData.thumbnail||''; $('createdInline').textContent=date(channelData.publishedAt); $('countryInline').textContent=channelData.country||'Not public';
  $('yt').href=channelData.youtubeUrl||('https://www.youtube.com/channel/'+channelData.channelId); $('banner').src=channelData.banner||'';
  if(!channelData.banner) $('banner').style.display='none';
  updateFavoriteButton(); renderSidebarFavorites();
  if($('profileButton')) $('profileButton').innerHTML='<span>U</span> '+(signedIn()?'Account⌄':'Sign In');
  if($('guestSignIn')) $('guestSignIn').style.display=signedIn()?'none':'inline-flex';
}

function setupNav(){
  const page=document.body.dataset.page;
  document.querySelectorAll('[data-page-link]').forEach(a=>{ const target=a.dataset.pageLink; a.href='/'+target+'?channel='+encodeURIComponent(channelData?.channelId||channelQuery); if(target===(page==='overview'?'analytics.html':page+'.html'))a.classList.add('active'); });
}

function renderOverview(){
  $('grade').textContent=channelData.grade||'—'; $('subs').textContent=channelData.hiddenSubscribers?'Hidden':num(channelData.subscribers); $('views').textContent=num(channelData.views); $('videos').textContent=num(channelData.videos); $('avg').textContent=num(channelData.averageViews); $('country').textContent=channelData.country||'Not public'; $('category').textContent=channelData.category||'—';
  const rows=history.slice(-7).reverse(); $('dailyBody').innerHTML=rows.length?rows.map((r,i)=>`<tr><td>${date(r.date)}</td><td class="positive">${i===rows.length-1?'—': '+'+Math.max(0,Number(r.subscribers||0)-Number(rows[i+1]?.subscribers||r.subscribers))}</td><td class="positive">${num(r.views)}</td><td>${num(r.videos)}</td></tr>`).join(''):'<tr><td colspan="4">No historical snapshots yet.</td></tr>';
}
function renderVideos(){
  const list=Array.isArray(channelData.recentVideos)?channelData.recentVideos:[]; const grid=$('videoGrid'); if(!grid)return;
  grid.innerHTML=list.map(v=>`<article class="video-card"><div class="video-thumb"><img src="${esc(v.thumbnail||'')}" alt="" loading="lazy"><span class="video-duration">${v.durationSeconds?Math.floor(v.durationSeconds/60)+':'+String(v.durationSeconds%60).padStart(2,'0'):''}</span></div><div class="video-card-body"><div class="video-card-title">${esc(v.title||'Untitled video')}</div><span class="video-type-label">${v.isShort?'Short':'Video'}</span><div class="video-stats-line">◉ ${num(v.views)} &nbsp; ♡ ${num(v.likes)} &nbsp; ◌ ${num(v.comments)}</div><div class="video-date">${date(v.publishedAt)}</div></div></article>`).join('') || '<div class="empty-state">No public videos returned.</div>';
}
function renderGrowth(){
  const rows=history; const s=$('growthSummary'); if(!s)return;
  if(rows.length<2){s.innerHTML='<div class="empty-state">Growth charts will appear after CreatorStats has stored more historical snapshots.</div>';return;}
  const first=rows[0], last=rows[rows.length-1]; const subDelta=Number(last.subscribers||0)-Number(first.subscribers||0), viewDelta=Number(last.views||0)-Number(first.views||0);
  s.innerHTML=`<div class="growth-kpis"><div><span>Subscriber change</span><strong>${subDelta>=0?'+':''}${num(subDelta)}</strong></div><div><span>View change</span><strong>${viewDelta>=0?'+':''}${num(viewDelta)}</strong></div><div><span>Snapshots</span><strong>${rows.length}</strong></div></div><div class="chart-bars">${rows.map(r=>`<div class="bar-item"><div class="bar" style="height:${Math.max(8,Math.min(100,(Number(r.subscribers||0)/Math.max(1,Number(last.subscribers||1)))*100))}%"></div><small>${new Date(r.date).toLocaleDateString(undefined,{month:'short',day:'numeric'})}</small></div>`).join('')}</div>`;
}
function renderProjection(){
  const p=history.length?null:null; const box=$('projectionGrid'); if(!box)return;
  box.innerHTML='<div class="projection-card"><span>30 days</span><strong>Collecting data</strong><small>CreatorStats needs more historical snapshots before showing a reliable projection.</small></div><div class="projection-card"><span>90 days</span><strong>Collecting data</strong><small>Projection confidence improves as your channel history grows.</small></div><div class="projection-card"><span>1 year</span><strong>Collecting data</strong><small>Long-term estimates will be enabled when enough history is available.</small></div>';
}
function renderLive(){ const n=$('liveSubs'); if(n)n.textContent=channelData.hiddenSubscribers?'Hidden':num(channelData.subscribers); if($('liveTime'))$('liveTime').textContent='Fetched '+new Date().toLocaleTimeString(); }
function renderAchievements(){
  const milestones=[1000,5000,10000,50000,100000,1000000]; const sub=Number(channelData.subscribers||0); $('achievementGrid').innerHTML=milestones.map(m=>`<div class="achievement-card ${sub>=m?'unlocked':''}"><div class="achievement-icon">★</div><strong>${num(m)} subscribers</strong><span>${sub>=m?'Unlocked':'Locked'}</span></div>`).join('');
}
async function renderCompare(){
  const input=$('compareInput'); const box=$('compareResult'); if(!input||!box)return;
  async function run(){ const value=input.value.trim(); if(!value)return; box.innerHTML='<div class="loading-state">Comparing...</div>'; try{const other=await api('/api/public/youtube/channel?query='+encodeURIComponent(value)); box.innerHTML=`<div class="compare-grid"><div class="compare-person"><img src="${esc(channelData.thumbnail||'')}" alt=""><strong>${esc(channelData.title)}</strong><span>${num(channelData.subscribers)} subscribers</span><span>${num(channelData.views)} views</span><span>${num(channelData.videos)} videos</span></div><div class="compare-vs">VS</div><div class="compare-person"><img src="${esc(other.thumbnail||'')}" alt=""><strong>${esc(other.title)}</strong><span>${num(other.subscribers)} subscribers</span><span>${num(other.views)} views</span><span>${num(other.videos)} videos</span></div></div>`;}catch(e){box.innerHTML='<div class="empty-state">'+esc(e.message)+'</div>';}}
  $('compareButton').onclick=run;
}

async function load(){
  try{ channelData=await api('/api/public/youtube/channel?query='+encodeURIComponent(channelQuery)); await loadFavorites(); const h=await api('/api/public/youtube/history?channelId='+encodeURIComponent(channelData.channelId)); history=Array.isArray(h?.history)?h.history:[]; renderShell(); setupNav(); const page=document.body.dataset.page; if(page==='overview')renderOverview(); if(page==='videos')renderVideos(); if(page==='growth')renderGrowth(); if(page==='projections')renderProjection(); if(page==='live')renderLive(); if(page==='achievements')renderAchievements(); if(page==='compare')renderCompare(); $('loading')?.classList.add('hidden-section'); $('dashboard')?.classList.remove('hidden-section'); }catch(e){ $('loading').textContent=e.message||'Unable to load analytics.'; }
}
window.toggleFavorite=toggleFavorite; window.openFavorites=openFavorites; window.toggleMenu=toggleMenu; window.globalAnalyze=globalAnalyze; window.go=go; window.login=login;
document.addEventListener('DOMContentLoaded',load);
