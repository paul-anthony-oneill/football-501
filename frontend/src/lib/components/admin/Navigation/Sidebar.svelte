<script lang="ts">
    import { page } from '$app/stores';

    export let open = true;

    const links = [
        { href: '/admin/categories', label: 'Categories', icon: '📁' },
        { href: '/admin/questions', label: 'Questions', icon: '❓' },
    ];

    $: currentPath = $page.url.pathname;
</script>

<aside class:open>
    <div class="logo">
        <h2>Football 501</h2>
        <span class="badge">Admin</span>
    </div>

    <nav>
        <ul>
            {#each links as link}
                <li>
                    <a
                        href={link.href}
                        class:active={currentPath.startsWith(link.href)}
                    >
                        <span class="icon">{link.icon}</span>
                        <span class="label">{link.label}</span>
                    </a>
                </li>
            {/each}
        </ul>
    </nav>

    <div class="footer">
        <a href="/" class="back-link">
            <span class="icon">⬅️</span>
            <span class="label">Back to Game</span>
        </a>
    </div>
</aside>

<style>
    aside {
        width: 250px;
        height: 100vh;
        background: var(--color-surface);
        border-right: 1px solid var(--color-outline);
        display: flex;
        flex-direction: column;
        transition: transform 0.3s ease;
        position: fixed;
        left: 0;
        top: 0;
        z-index: 100;
        box-shadow: var(--shadow-2);
    }

    .logo {
        padding: var(--space-lg);
        border-bottom: 1px solid var(--color-outline);
        display: flex;
        align-items: center;
        gap: var(--space-sm);
    }

    h2 {
        margin: 0;
        font-size: 1.25rem;
        color: var(--color-primary);
        font-weight: 800;
    }

    .badge {
        background: var(--color-primary-container);
        padding: 0.2rem 0.5rem;
        border-radius: var(--radius-xs);
        font-size: 0.65rem;
        color: var(--color-on-primary-container);
        text-transform: uppercase;
        font-weight: 800;
        letter-spacing: 0.5px;
    }

    nav {
        flex: 1;
        padding: var(--space-md);
    }

    ul {
        list-style: none;
        padding: 0;
        margin: 0;
    }

    li {
        margin-bottom: var(--space-xs);
    }

    a {
        display: flex;
        align-items: center;
        gap: var(--space-md);
        padding: var(--space-md);
        color: var(--color-on-surface-variant);
        text-decoration: none;
        border-radius: var(--radius-sm);
        transition: all 0.2s;
        font-weight: 500;
    }

    a:hover {
        background: var(--color-surface-variant);
        color: var(--color-on-surface);
    }

    a.active {
        background: var(--color-primary-container);
        color: var(--color-on-primary-container);
    }

    .icon {
        font-size: 1.25rem;
    }

    .footer {
        padding: var(--space-md);
        border-top: 1px solid var(--color-outline);
    }

    .back-link {
        color: var(--color-error);
        font-weight: 600;
    }

    .back-link:hover {
        background: var(--color-error-container);
        color: var(--color-on-error-container);
    }
</style>
