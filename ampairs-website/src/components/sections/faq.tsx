import { faqItems } from "@/lib/site";

export function FaqSection() {
  return (
    <section className="bg-muted py-24 dark:bg-[#0f172a]" aria-labelledby="faq">
      <div className="mx-auto max-w-5xl px-6">
        <div className="mx-auto max-w-2xl text-center">
          <h2 id="faq" className="text-3xl font-semibold sm:text-4xl">
            Frequently asked questions
          </h2>
        </div>
        <dl className="mt-12 space-y-6">
          {faqItems.map((faq) => (
            <div
              key={faq.question}
              className="rounded-3xl border border-white/10 bg-background px-6 py-6 shadow-sm"
            >
              <dt className="text-lg font-semibold">{faq.question}</dt>
              <dd className="mt-3 text-sm leading-6 text-foreground/70">
                {faq.answer}
              </dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
