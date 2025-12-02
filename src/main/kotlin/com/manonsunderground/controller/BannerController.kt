package com.manonsunderground.controller

import com.manonsunderground.service.BannerService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/banner")
class BannerController(
    private val bannerService: BannerService
) {

    @GetMapping("/{ip}/{port}/banner.webp")
    fun getBanner(
        @PathVariable ip: String,
        @PathVariable port: Int
    ): ResponseEntity<ByteArray> {
        val (image, contentType) = bannerService.generateBanner(ip, port)
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(image)
    }

    @GetMapping("/{ip}/{port}/widget")
    fun getWidgetHtml(
        @PathVariable ip: String,
        @PathVariable port: Int
    ): ResponseEntity<String> {
        val data = bannerService.getWidgetData(ip, port)
        
        // Calculate progress width
        val progress = if (data.maxPlayers > 0) (data.currentPlayers.toDouble() / data.maxPlayers.toDouble() * 100).toInt() else 0
        
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <script src="https://cdn.tailwindcss.com"></script>
                <script>
                    tailwind.config = {
                      theme: {
                        extend: {
                          fontFamily: {
                            sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
                          }
                        }
                      }
                    }
                    function copyIp() {
                        const ip = "${data.ip}:${data.port}";
                        navigator.clipboard.writeText(ip).then(() => {
                            const btn = document.getElementById('copyBtn');
                            const originalText = btn.innerHTML;
                            btn.innerText = 'Copied!';
                            setTimeout(() => {
                                btn.innerHTML = originalText;
                            }, 2000);
                        });
                    }
                </script>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;700&display=swap');
                    body { margin: 0; padding: 0; background-color: transparent; }
                </style>
            </head>
            <body class="antialiased p-4 flex justify-center items-center">
                <!-- Card Container -->
                <div class="relative w-full max-w-[400px] rounded-xl overflow-hidden bg-slate-900 text-white shadow-2xl font-sans group border border-slate-700/50 flex flex-col">
                    
                    <!-- Header Section (Map Background) -->
                    <div class="relative h-[200px] shrink-0">
                        <!-- Background Image with Overlay -->
                        <div class="absolute inset-0 bg-cover bg-center transition-transform duration-700 group-hover:scale-105" 
                             style="background-image: url('${data.mapImageBase64}');">
                        </div>
                        <!-- Gradient Overlay -->
                        <div class="absolute inset-0 bg-gradient-to-b from-slate-900/20 via-slate-900/50 to-slate-900"></div>
                        
                        <!-- Content Overlay -->
                        <div class="relative p-5 flex flex-col h-full justify-between z-10">
                            <!-- Top Row: Flag & Ping -->
                            <div class="flex justify-between items-start">
                            <!-- Flag -->
                            <div class="flex items-center gap-2 pt-1 pl-1">
                                ${if (data.country != "UNK") 
                                    """<img src="https://flagcdn.com/h20/${data.country.lowercase()}.png" alt="${data.country}" class="h-5 w-auto drop-shadow-md" title="${data.country}">"""
                                  else 
                                    """<span class="font-bold text-sm drop-shadow-md">UNK</span>"""
                                }
                            </div>
                            
                            <!-- Ping / Status -->
                            <div class="flex items-center gap-1.5 bg-black/40 backdrop-blur-sm px-2 py-1 rounded border border-white/10 text-xs font-medium text-green-400">
                                <div class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></div>
                                <span>Online</span>
                            </div>
                        </div>

                        <!-- Middle: Server Info -->
                        <div class="mt-2">
                            <h3 class="font-bold text-xl leading-tight mb-2 text-white drop-shadow-lg truncate" title="${data.serverName}">
                                ${data.serverName}
                            </h3>
                            
                            <div class="flex flex-wrap gap-2 text-[11px] font-bold tracking-wide uppercase">
                                <!-- Game Mode Badge -->
                                <span class="bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 px-2 py-1 rounded">
                                    ${data.gameMode}
                                </span>
                                
                                <!-- Map Name Badge -->
                                <span class="bg-slate-700/40 text-slate-300 border border-slate-600/30 px-2 py-1 rounded flex items-center gap-1">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 opacity-70" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 7m0 13V7" />
                                    </svg>
                                    ${data.mapName}
                                </span>
                            </div>
                        </div>

                        </div>
                    </div>

                    <!-- Body Section (Dark Background) -->
                    <div class="bg-slate-900 p-5 pt-2 space-y-2">
                        <!-- IP & Copy -->
                        <div class="space-y-2">
                            <div class="flex items-stretch gap-2 h-9">
                                <div class="flex-1 bg-slate-950/50 border border-slate-800 rounded flex items-center px-3 text-xs font-mono text-slate-400 select-all">
                                    ${data.ip}:${data.port}
                                </div>
                                <button id="copyBtn" onclick="copyIp()" class="bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 px-4 rounded text-xs font-bold transition-all flex items-center gap-1.5 active:scale-95 cursor-pointer">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                                    </svg>
                                    Copy
                                </button>
                            </div>

                            <!-- Players Progress -->
                            <div>
                                <div class="flex justify-between items-end mb-1.5">
                                    <div class="flex items-center gap-1.5 text-xs text-slate-400 font-medium">
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                                        </svg>
                                        Players
                                    </div>
                                    <div class="text-sm font-bold text-green-400">
                                        ${data.currentPlayers} <span class="text-slate-600 font-normal">/ ${data.maxPlayers}</span>
                                    </div>
                                </div>
                                <div class="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
                                    <div class="h-full bg-green-500 shadow-[0_0_10px_rgba(34,197,94,0.5)] rounded-full transition-all duration-1000" style="width: $progress%"></div>
                                </div>
                            </div>
                        </div>

                        <!-- Player List -->
                        <div class="border-t border-slate-800 pt-2">
                             <div class="flex items-center gap-2 mb-2">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0z" />
                                </svg>
                                <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">Online Players</span>
                            </div>
                            
                            <div class="bg-slate-950/30 rounded border border-slate-800 h-[240px] overflow-y-auto pr-1 space-y-0 custom-scrollbar">
                                ${if (data.players.isEmpty()) 
                                    "<div class='text-xs text-slate-600 text-center py-8 italic'>No players online</div>" 
                                  else 
                                    data.players.joinToString("") { 
                                        """<div class="text-xs text-slate-300 px-3 py-0.5 hover:bg-slate-800/50 rounded transition-colors flex items-center gap-2 border-b border-slate-800/50 last:border-0">
                                            <div class="w-1.5 h-1.5 rounded-full bg-green-500/50"></div>
                                            <span class="truncate">$it</span>
                                        </div>"""
                                    }
                                }
                            </div>
                        </div>

                        <!-- Footer -->
                        <div class="text-center pt-1">
                            <a href="https://allied-intel.appelpitje.workers.dev/" target="_blank" class="text-[10px] font-bold text-sky-500 hover:text-sky-400 transition-colors uppercase tracking-widest">
                                Allied Intel
                            </a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html)
    }
}
