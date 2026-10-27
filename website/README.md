# Twofold website

The marketing site for Twofold (the Folio app in this repository, sold on Google Play as Twofold). Plain Vite with TypeScript, no framework, served as static assets by Cloudflare Workers (free plan).

## Develop

```bash
npm install
npm run dev
```

`npm run cf:dev` builds and serves `dist/` through Wrangler, which is how Cloudflare will serve it, including `public/_headers` and the 404 page.

## Layout

- `index.html` is the landing page; `404.html` is served for any path that doesn't exist.
- `privacy.html` and `permissions.html` render the repository's `PRIVACY.md` and `PERMISSIONS.md` at build time, through the `<!-- markdown:FILE -->` marker and the small plugin in `vite.config.ts`, so the policy has one source. Links to either file become `/privacy` and `/permissions`; other relative links point at GitHub.
- `src/main.ts` and `src/style.css` are shared by every page.
- `src/images/` holds WebP copies of the store screenshots in `fastlane/metadata/android/en-US/images/` and the icon at small sizes. Vite fingerprints them into `/assets/`, which `_headers` caches for a year.
- `public/` is copied to the root of the site as is. `icon.png` is the store icon; `og.png` is the 1200 × 630 share card.
- A new page is a new `.html` file here plus an entry in `build.rolldownOptions.input` in `vite.config.ts`.
- The Get it on Google Play buttons are drawn in CSS for now. Google's brand rules ask for the official badge, so swap it in before launch.

## Deploy

One time: in the Cloudflare dashboard, Workers & Pages › Create › Import a repository, pick this repository, and set

- Root directory: `website`
- Build command: `npm run build`
- Deploy command: `npx wrangler deploy`

Every push to `main` then deploys, and other branches get preview URLs. Add the custom domain under the Worker's Settings › Domains & Routes.

To deploy by hand instead: `npx wrangler login`, then `npm run deploy`.

Keep Apple's names (iPhone, iOS, Spotlight, Dynamic Island, Liquid Glass) out of the copy, the same as the store listing.
