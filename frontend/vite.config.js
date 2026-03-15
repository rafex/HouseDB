import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

function buildSeoAssets(siteUrl) {
  const normalizedSiteUrl = siteUrl.replace(/\/$/, '')
  const indexableUrls = [`${normalizedSiteUrl}/`, `${normalizedSiteUrl}/login`]
  const robots = [
    'User-agent: *',
    'Allow: /',
    'Disallow: /casas',
    'Disallow: /locaciones',
    'Disallow: /objetos',
    'Disallow: /usuarios-api',
    `Sitemap: ${normalizedSiteUrl}/sitemap.xml`,
    '',
  ].join('\n')

  const sitemap = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ...indexableUrls.map(
      (url) => `  <url><loc>${url}</loc><changefreq>weekly</changefreq><priority>0.8</priority></url>`,
    ),
    '</urlset>',
    '',
  ].join('\n')

  return {
    name: 'housedb-seo-assets',
    generateBundle() {
      this.emitFile({
        type: 'asset',
        fileName: 'robots.txt',
        source: robots,
      })
      this.emitFile({
        type: 'asset',
        fileName: 'sitemap.xml',
        source: sitemap,
      })
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_BASE_URL || 'http://localhost:8080'
  const siteUrl = env.VITE_SITE_URL || 'https://housedb.v1.rafex.cloud'

  return {
    plugins: [
      vue(),
      buildSeoAssets(siteUrl),
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['favicon.svg'],
        manifest: {
          name: 'HouseDB',
          short_name: 'HouseDB',
          description: 'Inventario domestico para recordar lo que tienes y donde esta.',
          theme_color: '#18363a',
          background_color: '#f2f5f7',
          display: 'standalone',
          start_url: '/',
          icons: [
            {
              src: '/favicon.svg',
              sizes: 'any',
              type: 'image/svg+xml',
              purpose: 'any',
            },
          ],
        },
      }),
    ],
    server: {
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          secure: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
    define: {
      __HOUSEDB_SITE_URL__: JSON.stringify(siteUrl),
    },
  }
})
