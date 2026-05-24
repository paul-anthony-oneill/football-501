"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/admin/categories", label: "Categories", icon: "📁" },
  { href: "/admin/questions", label: "Questions", icon: "❓" },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside
      className="w-[250px] h-screen bg-[var(--color-surface)] border-r border-[var(--color-outline)] flex flex-col fixed left-0 top-0 z-[100] shadow-[var(--shadow-2)]"
    >
      {/* Logo */}
      <div className="px-6 py-5 border-b border-[var(--color-outline)] flex items-center gap-2">
        <h2 className="m-0 text-xl text-[var(--color-primary)] font-extrabold">
          Football 501
        </h2>
        <span className="bg-[var(--color-primary-container)] text-[var(--color-on-primary-container)] text-[0.65rem] uppercase font-extrabold tracking-[0.5px] px-2 py-[0.2rem] rounded-[var(--radius-xs)]">
          Admin
        </span>
      </div>

      {/* Nav */}
      <nav className="flex-1 p-4">
        <ul className="list-none p-0 m-0">
          {links.map((link) => {
            const isActive = pathname.startsWith(link.href);
            return (
              <li key={link.href} className="mb-1">
                <Link
                  href={link.href}
                  className={[
                    "flex items-center gap-4 px-4 py-4 rounded-[var(--radius-sm)] font-medium no-underline transition-all duration-200",
                    isActive
                      ? "bg-[var(--color-primary-container)] text-[var(--color-on-primary-container)]"
                      : "text-[var(--color-on-surface-variant)] hover:bg-[var(--color-surface-variant)] hover:text-[var(--color-on-surface)]",
                  ].join(" ")}
                >
                  <span className="text-xl">{link.icon}</span>
                  <span>{link.label}</span>
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      {/* Footer */}
      <div className="p-4 border-t border-[var(--color-outline)]">
        <Link
          href="/"
          className="flex items-center gap-4 px-4 py-4 rounded-[var(--radius-sm)] font-semibold no-underline text-[var(--color-error)] hover:bg-[var(--color-error-container)] hover:text-[var(--color-on-error-container)] transition-all duration-200"
        >
          <span className="text-xl">⬅️</span>
          <span>Back to Game</span>
        </Link>
      </div>
    </aside>
  );
}
