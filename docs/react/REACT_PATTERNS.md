# React Patterns — Field Guide for Backend Engineers

> **Audience:** You know Spring Boot well. You understand HTTP, services, repositories, dependency injection. You've barely touched React.  
> **Goal:** Get fluent with the patterns actually used in `frontend-react/` — nothing more.

---

## 0. The One Thing You Must Accept First

In React, **the UI is a function of state**.

```
UI = f(state)
```

Every time state changes, React calls your function again and redraws only the parts that changed. You don't manipulate the DOM — you describe what the screen *should* look like, and React figures out the diff.

Think of it like this: instead of writing `UPDATE` SQL statements when data changes, you just re-run a `SELECT` and React applies the diff automatically.

---

## 1. `useState` — Reactive Fields

**What it is:** A piece of data that, when changed, causes the component to re-render.

**Backend analogy:** Imagine a Spring `@Service` where every time you call a setter, the response is automatically recalculated and sent to the client. That's `useState`.

```tsx
// frontend-react/src/app/admin/categories/page.tsx  lines 22–24
const [categories, setCategories] = useState<Category[]>([]);
const [loading, setLoading]       = useState(true);
const [showCreate, setShowCreate] = useState(false);
```

**Rules:**
- Always use the setter (`setCategories`). Never mutate the value directly.
- Multiple `useState` calls = multiple independent pieces of state.
- The initial value (in the brackets) is only used on the first render.

```tsx
// ✅ Correct — React sees the change and re-renders
setCategories(newList);

// ❌ Wrong — React doesn't know this happened, no re-render
categories.push(newCategory);
```

**One gotcha — setting state is async:**
```tsx
setLoading(true);
console.log(loading); // still prints false! Update is queued, not instant.
```
React batches state updates. Don't read the variable immediately after setting it.

---

## 2. `"use client"` — Next.js App Router Directive

Every file in `frontend-react/src/app/` that uses `useState`, `useEffect`, event handlers, or browser APIs needs `"use client"` at the very top.

```tsx
// All our pages start with this
"use client";
```

**Why it exists:** Next.js 16 can render components on the *server* (faster initial page load, better SEO). But server components can't have state or click handlers — they have no browser. `"use client"` opts the whole file into browser-side rendering.

**Rule of thumb for this project:** Every page and every interactive component has `"use client"`. You don't need to think hard about it yet — just add it when the build complains.

---

## 3. Controlled Inputs — Forms

**The problem React solves:** In HTML, a `<input>` manages its own value internally. React wants *one source of truth* — the value lives in state, not in the DOM.

**Backend analogy:** Think of it like binding a form field to a DTO field. The DTO is the source of truth. The input just displays it.

```tsx
// frontend-react/src/components/admin/forms/CategoryForm.tsx  lines 23–25
const [name, setName] = useState(category?.name ?? "");
const [slug, setSlug] = useState(category?.slug ?? "");
```

```tsx
// The input mirrors state, and reports changes back up
<input
  value={name}           // ← reads from state
  onChange={e => setName(e.target.value)}  // ← writes to state
/>
```

**Mental model:** `value` = what the field *shows*. `onChange` = what happens when the user types. Both wired to the same `useState` variable.

**The slug auto-generation pattern** (`CategoryForm.tsx` lines 29–39) is a great example: when `name` changes, you compute `slug` in the same handler — no reactive magic needed, just a function.

```tsx
function handleNameChange(val: string) {
  setName(val);
  if (!isEdit) {
    setSlug(val.toLowerCase().replace(/[^a-z0-9]+/g, "-"));
  }
}
```

---

## 4. `useEffect` — Lifecycle Hooks

**What it is:** Code that runs *after* the component renders. Replaces `@PostConstruct`, event listeners, and async initialisation.

```tsx
// frontend-react/src/app/admin/categories/page.tsx  lines 44–46
useEffect(() => {
  loadCategories();
}, [loadCategories]);   // ← dependency array
```

**The dependency array is the key:**

| Array | Meaning |
|---|---|
| `[]` | Run once on mount — like `@PostConstruct` |
| `[someValue]` | Run whenever `someValue` changes |
| *(omitted)* | Run after every render — almost never what you want |

**Cleanup:** Return a function from `useEffect` to clean up when the component is removed from the page. See the debounce cleanup in `PlayerSearch.tsx` lines 70–74:

```tsx
useEffect(() => {
  return () => {                              // ← this runs on unmount
    if (debounceRef.current) clearTimeout(debounceRef.current);
  };
}, []);
```

**Auto-dismiss toasts** (`ToastContext.tsx` lines 36–39) is another clean example:
```tsx
useEffect(() => {
  const timer = setTimeout(onClose, 3000);
  return () => clearTimeout(timer);   // ← cancel if the toast is removed early
}, [onClose]);
```

**Rule:** If you fetch data on page load, you almost always write:
```tsx
useEffect(() => {
  fetchSomething();
}, []);
```

---

## 5. `useCallback` — Stable Function References

**The problem:** Every time React re-renders a component, it recreates every function defined inside it. This is normally fine — but it breaks `useEffect` dependency arrays, because React compares by reference and thinks the function changed.

**Symptom without `useCallback`:** Your effect runs in an infinite loop.

```
render → create loadCategories → useEffect sees new function → runs effect → setState → render → repeat
```

**Fix:**
```tsx
// frontend-react/src/app/admin/categories/page.tsx  lines 32–42
const loadCategories = useCallback(async () => {
  setLoading(true);
  try {
    const data = await adminApi.listCategories();
    setCategories(data);
  } catch (err) {
    addToast((err as Error).message, "error");
  } finally {
    setLoading(false);
  }
}, [addToast]);   // ← only recreate if addToast changes (it won't)
```

**Mental model:** `useCallback` is a memo around a function. React returns the *same function object* across renders unless the dependencies change.

**When to use it:** Only when the function goes into a `useEffect` dependency array, or is passed as a prop to a child that might re-render unnecessarily. Don't use it everywhere — it adds complexity for no gain in simple cases.

---

## 6. `useRef` — Mutable Fields That Don't Trigger Re-Renders

**What it is:** A container (`.current`) that you can read and write freely, but changing it does *not* cause a re-render.

**Two uses in this project:**

**Use 1 — DOM element references** (like `getElementById` but React-flavoured):
```tsx
// frontend-react/src/components/game/PlayerSearch.tsx  lines 31, 80, 101
const inputRef = useRef<HTMLInputElement>(null);

// Later, focus the input programmatically:
inputRef.current?.focus();

// Wire to the DOM element:
<input ref={inputRef} ... />
```

**Use 2 — Mutable values that shouldn't trigger re-renders** (the debounce timer):
```tsx
// PlayerSearch.tsx  line 30
const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

// Clear previous timer, start a new one
if (debounceRef.current) clearTimeout(debounceRef.current);
debounceRef.current = setTimeout(() => fetchSuggestions(val), 300);
```

**Why not `useState` for the timer?** Setting state causes a re-render. Storing a timer ID in state would re-render the component every time you start a new debounce — which defeats the purpose.

**Mental model:** `useRef` is like a `private` field on a class. You can read/write it anytime. React ignores it for rendering purposes.

---

## 7. The Debounce Pattern (Putting 5 + 6 Together)

`PlayerSearch.tsx` is the best example of `useRef` + `useCallback` + `useEffect` working together. Here's the full flow:

```tsx
// 1. Store the timer in a ref (won't cause re-renders)
const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

// 2. Stable function to fetch (won't break useEffect dependencies)
const fetchSuggestions = useCallback(async (query: string) => {
  // ... fetch from API
}, []);

// 3. On each keystroke: cancel old timer, start new one
function handleChange(e) {
  if (debounceRef.current) clearTimeout(debounceRef.current);
  debounceRef.current = setTimeout(() => fetchSuggestions(val), 300);
}

// 4. Cancel on unmount (cleanup)
useEffect(() => {
  return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
}, []);
```

**Result:** The API is only called 300ms after the user *stops* typing, not on every keystroke. Classic debounce — the React way.

---

## 8. `createContext` + `useContext` — Dependency Injection

**The problem:** You need toast notifications in many different pages. Passing `addToast` as a prop through every component in between is painful ("prop drilling"). This is the same problem Spring solves with `@Autowired`.

**React's solution:** Context. Put data in a provider at the top of the tree; any component below can consume it without explicit prop passing.

```
<ToastProvider>                    ← puts addToast into context
  <AdminLayout>
    <CategoriesPage>               ← useToast() pulls it back out
```

**Creating a context** (`ToastContext.tsx`):

```tsx
// 1. Define the shape
interface ToastContextValue {
  addToast: (message: string, type?: ToastType) => void;
  removeToast: (id: string) => void;
}

// 2. Create it (null = not yet provided)
const ToastContext = createContext<ToastContextValue | null>(null);

// 3. Provider: holds state, exposes it to the tree
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const addToast = useCallback((message, type = "info") => {
    setToasts(prev => [...prev, { id: ..., message, type }]);
  }, []);

  return (
    <ToastContext.Provider value={{ toasts, addToast, removeToast }}>
      {children}    {/* ← the entire app tree */}
    </ToastContext.Provider>
  );
}

// 4. Custom hook: makes consuming clean and safe
export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be inside <ToastProvider>");
  return ctx;
}
```

**Consuming it anywhere** (e.g. `categories/page.tsx` line 20):
```tsx
const { addToast } = useToast();

// Then anywhere:
addToast("Category deleted", "success");
```

**Analogy:** `ToastProvider` = a Spring `@Bean` definition. `useToast()` = `@Autowired`. The context is the application container.

**When to use context:** Shared UI state (toasts, modals, auth, themes). **Not** for everything — local UI state belongs in `useState` on the component that owns it.

---

## 9. Generic TypeScript Components

React components are just functions. TypeScript generics work exactly as they do in Java.

```tsx
// frontend-react/src/components/admin/DataTable.tsx  lines 8–20
interface DataTableProps<T extends { id: string }> {
  data: T[];
  renderActions?: (item: T) => React.ReactNode;
}

export default function DataTable<T extends { id: string }>({
  data,
  renderActions,
}: DataTableProps<T>) {
  // data is typed as T[] — full autocomplete in the IDE
}
```

**Using it** — TypeScript infers `T` from what you pass:
```tsx
// T is inferred as Category
<DataTable<Category>
  data={categories}
  renderActions={(item) => (
    <button onClick={() => handleEdit(item)}>✏️</button>
    //  item is typed as Category — IDE knows .name, .slug, etc.
  )}
/>
```

**Analogy:** Identical to `List<T>` in Java. The constraint `T extends { id: string }` is like `<T extends Identifiable>`.

**`React.ReactNode`** is the type for "anything React can render" — JSX, strings, null, arrays. It's the React equivalent of `Object` for render output.

---

## 10. `useParams` + `useRouter` — Navigation

These are Next.js hooks for reading the URL and navigating programmatically.

```tsx
// frontend-react/src/app/admin/questions/[id]/page.tsx  lines 30–32
const { id: questionId } = useParams<{ id: string }>();  // read URL param
const router = useRouter();                               // programmatic navigation
```

**`useParams`** reads the dynamic segments from the URL. For the file path `app/admin/questions/[id]/page.tsx`, the segment `[id]` maps to `params.id`.

**`useRouter`** for navigation:
```tsx
router.push("/admin/questions");        // navigate (adds to browser history)
router.replace("/admin/questions");     // navigate (replaces history entry)
router.back();                          // browser back button
```

**`<Link href="...">`** for navigation *in JSX* (replaces `<a href>`):
```tsx
import Link from "next/link";

<Link href="/admin/categories">Categories</Link>
```

Use `<Link>` in templates; use `router.push()` after async operations (e.g. "form saved → redirect").

---

## Quick Reference: Svelte → React Mental Model

| You knew (Svelte) | You now use (React) |
|---|---|
| `let x = 0` | `const [x, setX] = useState(0)` |
| `$: derived = x * 2` | `const derived = x * 2` (plain expression) |
| `$: { doSomething() }` | `useEffect(() => { doSomething() }, [deps])` |
| `bind:value` | `value={x} onChange={e => setX(e.target.value)}` |
| `on:click` | `onClick` |
| `<slot>` | `{children}` prop |
| `createEventDispatcher` | callback props (`onSave`, `onClose`) |
| `onMount(() => ...)` | `useEffect(() => ..., [])` |
| `$page.url.pathname` | `usePathname()` |
| `goto('/path')` | `router.push('/path')` |
| `$: reactiveStatement` | `useCallback` or `useMemo` |

---

## Where to Look in the Codebase

| Pattern | Best example file |
|---|---|
| `useState` basics | `src/app/admin/categories/page.tsx` |
| Controlled form inputs | `src/components/admin/forms/CategoryForm.tsx` |
| `useEffect` on mount | `src/app/admin/categories/page.tsx` line 44 |
| `useEffect` cleanup | `src/context/ToastContext.tsx` line 36 |
| `useCallback` + `useEffect` | `src/app/admin/categories/page.tsx` lines 32–46 |
| `useRef` (DOM) | `src/components/game/PlayerSearch.tsx` line 31 |
| `useRef` (mutable value) | `src/components/game/PlayerSearch.tsx` line 30 |
| Full debounce pattern | `src/components/game/PlayerSearch.tsx` |
| Context / DI | `src/context/ToastContext.tsx` |
| Consuming context | `src/app/admin/categories/page.tsx` line 20 |
| Generic components | `src/components/admin/DataTable.tsx` |
| `useParams` + `useRouter` | `src/app/admin/questions/[id]/page.tsx` lines 30–32 |
