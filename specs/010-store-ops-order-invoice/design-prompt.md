# Design Prompt — Store Operations: Order & Invoice (KMP, Material 3)

> Paste the block below into a new Claude session (ideally one that can build Artifacts). It is
> self-contained — it does not assume access to any repository. It asks for the UX flows **and** an
> interactive prototype for both the Order and the Invoice store-operations screens.

---

You are a senior product designer specializing in **point-of-sale / order-desk software** and
**Material 3**. Design the screens and end-to-end flows for a **store-operations Order and Invoice
feature** in a business-management app, then build an **interactive high-fidelity prototype**.

## Product context

- Users are **store staff** (counter/back-office) creating **sales orders** and **GST tax invoices**
  for customers in India. Think fast, keyboard-and-touch order entry — not a consumer checkout.
- The app is **Compose Multiplatform**, running on **Android phone, tablet, and Desktop**. Designs must
  be **adaptive**: a single-pane flow on compact (phone) and a **list-detail two-pane** layout on
  expanded (tablet/desktop). Desktop users use mouse + keyboard heavily (tab between fields, Enter to
  add a line).
- **Offline-first**: documents are created and fully usable with no network. Show clear sync state
  (saved-locally / syncing / synced / sync-failed) without being noisy.
- Visual language: **Material 3** (dynamic color, tonal surfaces, rounded components, M3 typography).
  Light theme default; support dark. Currency is **₹ (INR)**.

## What an Order and an Invoice are here

- **Order** = a working sales document (draft → confirmed). **Invoice** = the legal GST tax document.
- They can be created **independently**, OR an Invoice can be **generated from an Order** ("Create
  Invoice" on a saved order copies its lines/tax/discounts and cross-links them).
- Both have the **same line-item anatomy and totals math** — design them as one consistent pattern with
  two entry points, not two unrelated screens.

## The line-item anatomy (this is the core of the design)

Each document has a **customer**, a list of **line items**, document-level settings, and **totals**.

A **line item** captures, in this logical order:
1. **Product** — picked from the catalog via a searchable picker. The picker must also allow
   **creating a new product inline** (name, price, tax/HSN code, unit) without leaving the document.
2. **Variant** — if the product has variants, the staffer selects one (e.g., size/color).
3. **Unit of measure** — the staffer transacts in the product's **base unit or a derived unit**
   (e.g. "BOX" where 1 BOX = 12 PCS). Selecting a different unit rescales the price and shows the
   derived base quantity. Quantity input respects the unit's allowed decimal places.
4. **Quantity** and **unit price** — price auto-fills from the product but is **editable/overridable**.
5. **Line discount** — a percentage **or** a flat amount on that line.
6. **GST** — computed automatically and shown as a breakdown: **CGST + SGST** for intra-state sales, or
   **IGST** for inter-state (decided by the customer's state vs the seller's state). Each line shows its
   taxable value and tax.

Document-level controls:
- **Tax mode toggle**: **Tax-exclusive** (GST added on top of price) vs **Tax-inclusive** (price already
  includes GST, tax shown as extracted). A clear, prominent toggle — it changes every number.
- **Overall (whole-document) discount** — percentage or flat, on the whole bill.
- **Customer** selector (with a **walk-in** option capturing name/phone), and the customer's GSTIN/state
  which drives CGST/SGST-vs-IGST.
- **Totals panel**: subtotal (taxable), the GST component breakdown (CGST/SGST/IGST grouped), discounts,
  and the grand total — always reconciling and always visible while editing.
- **Invoice only**: a **GST invoice number** is assigned at save (sequential, works offline) and shown
  prominently; it must look official/printable.

## Key flows to design (show each as a step-by-step flow diagram + the screens)

1. **Create an order** — open → pick customer → add line(s) [pick product → variant → unit → qty →
   price → line discount] → review GST + totals → apply overall discount / toggle tax mode → save (offline).
2. **Create an invoice directly (POS-style)** — same, including walk-in customer; ends with an assigned
   invoice number and a "share/print" affordance.
3. **Add a product inline** during line entry — the minimal create-product sheet and return to the line.
4. **Convert an order to an invoice** — from a saved order, "Create Invoice" → confirm → numbered invoice.
5. **Browse/search** orders and invoices (list), open one (detail/read view), see status + sync state.

## Screens to deliver (compact + expanded for each)

- **Document list** (orders / invoices): search, status chips, sync indicators, FAB / "New".
- **Document editor** (the heart): header (customer, date, doc settings), the **line-items area**, the
  **add-line / product-picker** experience, the **per-line editor** (unit, qty, price, discount, GST),
  the **sticky totals panel**, and the save/confirm bar. Design the expanded (two-pane) and compact
  (single-pane, with a bottom-sheet line editor) variants.
- **Product picker** (searchable list + "Create new product" path) and the **inline create-product** sheet.
- **Unit selector** (base + derived units with conversion hint).
- **Document detail / read view** (a clean, printable-looking summary; invoice looks like a tax invoice).
- **Order→Invoice conversion** confirmation.

## States to cover

Empty (no lines), typing/calculating, a line with CGST/SGST vs a line with IGST, tax-inclusive vs
exclusive, line + overall discount applied, validation errors (missing tax code, qty below MOQ-style
warnings), saved-offline, syncing, sync-failed, and a fully synced invoice with its number.

## Deliverables

1. **Flow diagrams** for the 5 flows above (concise, labeled steps with decision points).
2. **A screen inventory** with a one-line purpose for each screen and its compact/expanded behavior.
3. **Annotated wireframes / layouts** for every screen and the key states above (M3 components, spacing,
   hierarchy; call out what's sticky, what scrolls, primary vs secondary actions).
4. **An interactive prototype** (single-file, self-contained — HTML + Tailwind, or a React artifact)
   demonstrating the **document editor** end-to-end for **both order and invoice**: add a line, pick a
   unit, edit price, add a line discount, toggle tax-inclusive/exclusive, apply an overall discount, and
   watch the GST breakdown + grand total update live. Include the adaptive layout (resize → phone vs
   desktop) and an example invoice with a number. Use realistic ₹ numbers that actually reconcile.
5. A short **interaction & accessibility note**: keyboard flow for desktop fast entry, touch targets,
   focus order, screen-reader labels for the GST breakdown and totals.

## Constraints / do-nots

- Material 3 only; no other design systems. Don't invent unrelated features (no payments, shipping,
  loyalty, promotions — those are separate). Keep money in ₹. Make the **GST breakdown and the
  inclusive/exclusive toggle first-class** — they're the differentiators of this screen. Optimize the
  editor for **speed of repeated line entry**, especially on desktop.

Start with the flows and screen inventory, then the wireframes, then build the interactive prototype.
