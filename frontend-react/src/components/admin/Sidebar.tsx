"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, useEffect } from "react";

const links = [
  { href: "/admin/categories", label: "Categories", icon: "📁" },
  { href: "/admin/questions",  label: "Questions",  icon: "❓" },
  { href: "/admin/templates",  label: "Templates",  icon: "🧩" },
  { href: "/admin/entities",   label: "Entities",   icon: "🔤" },
];

export default function Sidebar() {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    document.body.style.setProperty(
      "--sidebar-width",
      collapsed ? "60px" : "250px",
    );
  }, [collapsed]);

  return (
    <>
      {/* Mobile overlay */}
      {!collapsed && (
        <div
          className="fixed inset-0 bg-black/50 z-[90] lg:hidden"
          onClick={() => setCollapsed(true)}
        />
      )}

      <aside
        className={[
          "h-screen bg-[var(--color-surface)] border-r border-[var(--color-outline)] flex flex-col fixed left-0 top-0 z-[100] transition-all duration-300",
          collapsed
            ? "-translate-x-full lg:translate-x-0 lg:w-[60px]"
            : "w-[250px] translate-x-0",
        ].join(" ")}
        aria-label="Admin navigation"
      >
        {/* Header */}
        <div className="px-6 py-5 border-b border-[var(--color-outline)] flex items-center gap-2">
          {collapsed ? (
            <button
              onClick={() => setCollapsed(false)}
              className="text-[var(--color-primary)] text-xl bg-transparent border-none cursor-pointer p-0"
              aria-label="Expand sidebar"
            >
              &#9776;
            </button>
          ) : (
            <>
              <h2 className="m-0 text-xl text-[var(--color-primary)] font-extrabold whitespace-nowrap">
                Trivia 501
              </h2>
              <span className="bg-[var(--color-primary-container)] text-[var(--color-on-primary-container)] text-[0.65rem] uppercase font-extrabold tracking-[0.5px] px-2 py-[0.2rem] rounded-[var(--radius-xs)] whitespace-nowrap">
                Admin
              </span>
              <button
                onClick={() => setCollapsed(true)}
                className="ml-auto text-[var(--color-on-surface-variant)] bg-transparent border-none cursor-pointer p-0 text-lg hover:text-white transition-colors"
                aria-label="Collapse sidebar"
              >
                &#10005;
              </button>
            </>
          )}
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
                    title={collapsed ? link.label : undefined}
                    aria-current={isActive ? "page" : undefined}
                    className={[
                      "flex items-center gap-4 px-4 py-4 rounded-[var(--radius-sm)] font-medium no-underline transition-all duration-200",
                      isActive
                        ? "bg-[var(--color-primary-container)] text-[var(--color-on-primary-container)]"
                        : "text-[var(--color-on-surface-variant)] hover:bg-[var(--color-surface-variant)] hover:text-[var(--color-on-surface)]",
                    ].join(" ")}
                  >
                    <span className="text-xl flex-shrink-0" aria-hidden="true">{link.icon}</span>
                    {!collapsed && <span className="truncate">{link.label}</span>}
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
            title={collapsed ? "Back to Game" : undefined}
            className="flex items-center gap-4 px-4 py-4 rounded-[var(--radius-sm)] font-semibold no-underline text-[var(--color-error)] hover:bg-[var(--color-error-container)] hover:text-[var(--color-on-error-container)] transition-all duration-200"
          >
            <span className="text-xl flex-shrink-0" aria-hidden="true">⬅️</span>
            {!collapsed && <span>Back to Game</span>}
          </Link>
        </div>
      </aside>
    </>
  );
}
