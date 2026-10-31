# Twofold website

The marketing site for Twofold, at <https://twofoldlauncher.app> (the Folio app in this repository, sold on Google Play as Twofold). Plain Vite with TypeScript, no framework, served as static assets by Cloudflare Workers (free plan).

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

The site lives at <https://twofoldlauncher.app>, served by the Worker `folio-website`. It deploys by hand, from an up-to-date `main`, with Wrangler logged in (`npx wrangler whoami`; `npx wrangler login` if not):

```bash
bash tools/deploy-website.sh
```

The script refuses uncommitted changes to the site or to `PRIVACY.md` and `PERMISSIONS.md`, and refuses anything but `origin/main`, then builds, runs `wrangler deploy` and checks that every page answers on the live domain. `--dry-run` builds and checks the upload without sending it, and `--check` only checks the live site. A change to `PRIVACY.md` isn't public until the site is deployed, since the app and Play link to `/privacy`.

`routes` in `wrangler.jsonc` binds the domain to the Worker on deploy, so its zone has to be in the same Cloudflare account. The canonical links, `og:url`, `og:image` and `public/sitemap.xml` name the domain too, so a move touches all of them.

To deploy on every push to `main` instead, connect the repository under the Worker's Settings › Build in the Cloudflare dashboard with root directory `website`, build command `npm run build` and deploy command `npx wrangler deploy`.

Keep Apple's names (iPhone, iOS, Spotlight, Dynamic Island, Liquid Glass) out of the copy, the same as the store listing.
