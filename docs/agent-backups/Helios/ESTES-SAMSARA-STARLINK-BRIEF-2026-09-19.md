# Estes Samsara + Starlink brief

**For Chris Olsen — September 19, 2026 (CT)**  
**Style:** plain English; verified public facts only. **Evidence status: Soft-PASS.**

> **Samsara UI screenshot: PENDING from Chris.**

## 1) Samsara: what it is and what can realistically be built

### The simple picture

Think of Samsara as three pieces:

1. **A small truck computer (“Vehicle Gateway”)** reads truck data and GPS.
2. **The Samsara Driver App** runs on an approved phone/tablet. It is the driver-facing ELD, HOS clock, DVIR, messages, documents, vehicle/trailer selection, and routing tool.
3. **The Samsara cloud** stores the information for dispatch, safety, maintenance, and compliance.

The tablet connects to the Vehicle Gateway’s Wi-Fi. If cell service drops, the gateway and app can keep working and sync later. Samsara says its app works on supported iOS and Android devices; the gateway is what supplies the truck/ELD data. Sources: [Samsara ELD](https://www.samsara.com/products/telematics/eld-compliance), [Driver App guide](https://www.samsara.com/pdf/docs/new-experience-samsara-driver-app-guide-us-en.pdf).

I did not find public proof of Estes’s exact Samsara hardware, settings, or contract. This brief uses Chris’s description of the Estes tablet.

### Publicly known software stack

- **Driver tablet UI:** Samsara publicly says the Driver App was built with **React Native**. Its engineering post also says Samsara uses **React and TypeScript** for apps on devices it does not control, such as mobile and web.
- **Publicly described app tools:** Redux, offline reconciliation, navigation, event bus, authentication, storage, push notifications, Code Push, and a shared component library.
- **Samsara-controlled device code:** The same post says Samsara uses **Go** for code running on devices it controls. That does **not** prove the cloud backend is written in Go.
- **Backend/cloud language and framework:** **Unknown from the public sources checked.** Do not assume Node, Go, Java, Python, or a particular database.
- These engineering details come from a public post about a major app rewrite, so they are useful clues, not a promise that every current screen is unchanged. Source: [Samsara engineering post](https://medium.com/samsara-engineering/why-and-how-we-rewrote-our-biggest-mobile-app-from-scratch-346f05f5b57a).

### How it works end-to-end

Truck sensors and the Vehicle Gateway collect driving, location, and vehicle information. The tablet shows the driver the clocks, inspection steps, messages, and forms. The app can hold work while offline. When a connection returns, the gateway/app sends the saved information to Samsara’s cloud. Dispatch and safety staff see it in the dashboard. Other company systems can receive selected information through Samsara’s API or event messages.

### Real ways to build or connect

- **Direct API:** A company system can use Samsara’s HTTPS/JSON REST API with a secured API token. Example: a vehicle-location feed with a cursor for new updates. The older location feed recommends waiting at least five seconds when there is no new data. Source: [Locations API](https://developers.samsara.com/reference/getvehiclelocationsfeed).
- **Webhooks:** Samsara can send an HTTPS message to a company server when events occur, such as alerts or vehicle changes. Requests can be signature-checked. Source: [Webhooks](https://developers.samsara.com/docs/webhooks).
- **SDKs:** Official, documented SDKs are available for **TypeScript, Java, C#, and Python**. Source: [SDKs](https://developers.samsara.com/docs/sdks).
- **Marketplace/partner route:** Build an app in Samsara’s developer portal, use OAuth 2.0 with only the permissions needed, test with a pilot customer, then request Marketplace certification. Samsara also has a Technology Partner Program. Sources: [Marketplace apps](https://developers.samsara.com/docs/marketplace-apps), [certification](https://developers.samsara.com/docs/app-certification-process), [Technology Partner Program](https://developers.samsara.com/docs/technology-partner-program).
- **Internal overlay:** Estes could build a small web/tablet companion that reads approved Samsara data and applies Estes rules. Samsara also advertises Workflow Builder/App Designer for customizing parts of the mobile experience without code, subject to the company’s licenses and admin approval. This is not the same as owning or replacing Samsara’s ELD.
- **White-label:** I found **no public Samsara white-label/OEM promise**. A separate Estes-branded app that uses the API is realistic; rebranding Samsara’s Driver App is not something to assume. It would require Samsara’s written agreement.

### The one-digit trailer “misfold” problem

**Best first move:** document 10–20 real examples: what the trailer number should be, what the screen showed, where the wrong digit came from, and what correction was made. Do not auto-correct silently; the wrong trailer can become a compliance or maintenance mistake.

Three realistic paths:

1. **Samsara feature request (lowest build risk):** Ask for clearer full-number display, stronger confirmation, “nearby/last used” sorting, duplicate/one-digit warning, and an admin-controlled trailer master list. The current guide confirms trailer selection and allows a driver to correct trailer/form data, but it does not promise these proposed safeguards.
2. **Estes overlay (most control):** Use Samsara data plus Estes’s trailer list to show a simple “You picked 5317; did you mean 5387?” check. Require driver confirmation and send the result to the proper Estes/Samsara workflow. Keep the official ELD record authoritative; do not write around compliance controls.
3. **Samsara partner app (best if it helps many customers):** Build the overlay as an OAuth Marketplace app, pilot it at Estes, and seek certification. This takes security review, support ownership, and a real pilot—not just a clever screen.

## 2) Starlink for trucking versus Qualcomm MCP

### What Starlink offers

**Yes:** Starlink Business sells an in-motion **Performance Kit** for land mobility. Starlink says it is built for harsh weather, vibration, and vehicle motion; it supports AC or DC power, remote fleet management, and up to 400+ Mbps download capability. The specification sheet lists roughly **75–100 W average power**, IP69K with cables installed, and a 10-year design life. It needs a reasonably clear view of the sky; tunnels, buildings, trees, and mounting position still matter. Sources: [Land Mobility](https://starlink.com/business/mobility), [Performance specifications](https://starlink.com/public-files/specification_sheet_performance.pdf).

**Plans/cost:** Starlink’s official page says “contact sales,” so there is no universal public 2026 truck price to use as a budget. A separate EpicVue trucking offer advertises **$99/month for 250 GB**, with a listed **$239 hardware charge**; treat that as a reseller offer, not a guaranteed Starlink/Estes quote. Source: [EpicVue](https://epicvue.com/starlink/).

**API:** Starlink’s Telemetry API can stream terminal health about every 15 seconds: throughput, latency, packet loss, obstruction, uptime, alerts, and an approximate H3 map cell. It uses a service-account bearer token and continuous polling. It is mainly a **“is the satellite link healthy?” API**. It is not a complete vehicle-GPS or turn-by-turn emergency-routing API. Use Samsara or another GPS system for the truck’s exact location and a separate dispatch/routing service for emergency handling. Source: [Starlink Telemetry API](https://starlink.readme.io/docs/telemetry-api).

### Simple comparison

| Question | Starlink Performance | Qualcomm/Omnitracs MCP |
|---|---|---|
| Main job | Broadband internet in remote places | Purpose-built in-cab fleet/ELD terminal |
| Connection | Satellite; needs sky view | Historically cellular/digital wireless plus GPS |
| Driver work | Internet, messages, apps, emergency communications | HOS/ELD, two-way dispatch, tracking, navigation, reports |
| Exact truck tracking | Not by itself; use Samsara/GPS | Built-in GPS/fleet tracking was a core feature |
| API | Public telemetry API for terminal health | Public/current Estes integration details unknown |
| 2026 status | Current Starlink Business product | MCP is a legacy Qualcomm/Omnitracs line; current availability/support is unknown |

The MCP50 brochure describes tracking, two-way messaging, HOS, navigation, safety, and fleet-software integration. The product was later associated with Omnitracs, so “Qualcomm” may be the shop-floor name rather than the current vendor. Sources: [MCP50 brochure](https://www.qualcomm.com/media/documents/files/mcp50-brochure.pdf), [FreightWaves history](https://ratings.freightwaves.com/reviews/omnitracs-mcp-qualcomm/).

### Leadership pitch skeleton for Estes

**The ask:** Run a small, reversible pilot—not a fleetwide swap.

- **Cost:** Get an Estes-specific Starlink quote including dish, mount, DC wiring, install, monthly data, support, and replacement. Compare it with the cost Estes already pays for MCP/Samsara/cellular service. Do not compare a broadband dish to an ELD one-for-one.
- **Coverage:** Pick routes with known cellular dead spots. Starlink may fill those gaps, but sky blockage can create new gaps.
- **Reliability:** Keep Samsara/MCP as the compliance and location system. Measure Starlink uptime, latency, packet loss, obstruction time, and data use. Its API can provide those link-health numbers.
- **Emergency:** Test whether a driver can reach dispatch and the company’s emergency process in a dead zone. Starlink provides a data connection; it does not replace Estes’s emergency policy, exact GPS source, or dispatch/routing system.
- **One-employee champion path:** Chris gathers a short dead-zone log, recruits 3–5 volunteer trucks, gets IT/safety/fleet/procurement approval, and proposes success rules: fewer failed messages, working emergency contact, acceptable install/power burden, and no ELD/compliance regression. After 30 days, present driver feedback and measured costs. If it fails, unplug the pilot; if it works, leadership has evidence instead of a sales promise.
