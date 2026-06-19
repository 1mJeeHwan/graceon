"use client";

import Link from "next/link";
import { Disc3, MapPin, Megaphone, Search, ShoppingBag } from "lucide-react";
import { ThemeToggle } from "./ThemeToggle";
import { Logo } from "./Logo";

/** Top app bar: logo left, search right. Sticky within the phone frame. */
export function AppBar() {
  return (
    <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-border/60 bg-bg/85 px-5 backdrop-blur-md">
      <Link href="/" aria-label="StreamHub 홈">
        <Logo />
      </Link>
      <nav className="flex items-center gap-4">
        <Link href="/churches" aria-label="교회찾기" className="text-active transition-colors hover:text-primary">
          <MapPin className="h-5 w-5" />
        </Link>
        <Link href="/albums" aria-label="음반" className="text-active transition-colors hover:text-primary">
          <Disc3 className="h-5 w-5" />
        </Link>
        <Link href="/goods" aria-label="굿즈샵" className="text-active transition-colors hover:text-primary">
          <ShoppingBag className="h-5 w-5" />
        </Link>
        <Link href="/campaigns" aria-label="이벤트" className="text-active transition-colors hover:text-primary">
          <Megaphone className="h-5 w-5" />
        </Link>
        <Link href="/search" aria-label="검색" className="text-active transition-colors hover:text-primary">
          <Search className="h-5 w-5" />
        </Link>
        <ThemeToggle />
      </nav>
    </header>
  );
}
