import type { ReactNode } from "react";

type PageIntroProps = {
  eyebrow?: string;
  title: string;
  description?: string;
  className?: string;
  children?: ReactNode;
};

export function PageIntro({
  eyebrow,
  title,
  description,
  className = "",
  children,
}: PageIntroProps) {
  return (
    <div className={`space-y-5 ${className}`}>
      {eyebrow ? (
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-primary">
          {eyebrow}
        </p>
      ) : null}
      <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
        {title}
      </h1>
      {description ? (
        <p className="text-base text-foreground/70">{description}</p>
      ) : null}
      {children}
    </div>
  );
}
