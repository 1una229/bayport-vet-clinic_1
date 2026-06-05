# Email sender profile picture (logo next to sender name)

The **circle logo beside the sender name** in Gmail (like Paw Care’s inbox) is **not** controlled by the email HTML template. Gmail and other providers choose it from the **From address** and your domain setup.

## Why Bayport may show a generic icon today

If `RESEND_FROM` is `Bayport Veterinary Clinic <onboarding@resend.dev>`, mail is sent from Resend’s shared test domain. You cannot set a custom profile picture for `@resend.dev`.

## Option A — Same as Paw Care (Gmail profile photo) — best if you send from Gmail

Works when mail is sent **from** `bayportveterinaryclinic@gmail.com` (local app or SMTP on a host that allows port 587).

1. Open [Google Account → Personal info](https://myaccount.google.com/personal-info).
2. Upload your Bayport logo as the **profile picture** for `bayportveterinaryclinic@gmail.com`.
3. Configure the app to send with that Gmail account (`SPRING_MAIL_USERNAME` / app password).
4. Recipients on Gmail (mobile and opened messages on desktop) will often see that photo next to your clinic name.

> **Render FREE** blocks Gmail SMTP. This option applies to **local/desktop** use, or cloud after upgrading Render / using another host that allows SMTP.

## Option B — Custom domain + Resend (recommended for online / Render)

1. Buy or use a domain you control (e.g. `bayportvetclinic.com`).
2. In [Resend → Domains](https://resend.com/domains), add a subdomain (e.g. `mail.bayportvetclinic.com`) and add the DNS records Resend shows (SPF, DKIM).
3. In Render, set:
   ```
   RESEND_FROM=Bayport Veterinary Clinic <reminders@mail.bayportvetclinic.com>
   ```
4. Redeploy the API.

Then add **BIMI** so major providers can show your logo in the inbox (see Option C).

## Option C — BIMI (logo for all addresses on your domain)

BIMI tells Gmail, Yahoo, Apple Mail, etc. to show your verified logo for mail from your domain.

**Prerequisites**

- Domain verified in Resend (Option B).
- **DMARC** policy `p=quarantine` or `p=reject` (not `p=none`).
- SPF and DKIM passing (Resend DNS setup).
- Logo as **SVG Tiny PS**, square, solid background, under 32 KB.

**Bayport BIMI logo file (hosted on Netlify)**

After deploy, the file is available at:

```
https://YOUR-SITE.netlify.app/assets/bimi-logo.svg
```

Example DNS TXT record (replace domain and URL):

| Host | Type | Value |
|------|------|--------|
| `default._bimi.mail.bayportvetclinic.com` | TXT | `v=BIMI1; l=https://adorable-daifuku-3966e7.netlify.app/assets/bimi-logo.svg;` |

- Propagation can take **24–48 hours**.
- **Gmail** may require a Verified Mark Certificate (VMC) or Common Mark Certificate (CMC) for the logo in the **inbox list**; Apple Mail and Yahoo often show BIMI sooner without VMC.
- See [Google BIMI help](https://support.google.com/a/answer/10911320) and [Resend: avatar](https://resend.com/docs/knowledge-base/how-do-i-send-with-an-avatar).

## Option D — Gravatar (some clients only)

Thunderbird, Airmail, Postbox, and a few others use [Gravatar](https://gravatar.com):

1. Create a Gravatar account with the **same email** you use in `RESEND_FROM`.
2. Upload the Bayport logo.
3. Verify that address.

This does **not** replace Gmail’s profile photo for most Gmail users.

## Summary

| Goal | What to do |
|------|------------|
| Logo like Paw Care (Gmail) | Profile photo on `bayportveterinaryclinic@gmail.com` + send from that Gmail (local SMTP) |
| Logo on Render / Resend | Own domain in Resend + BIMI DNS + optional VMC/CMC for Gmail |
| Logo inside the email body | Already done (header + footer in reminder template) |

The **email body** logo is set in the backend (`bayport.email.logo-url`). The **inbox profile** logo requires one of the options above.
