<script lang="ts">
    import Sidebar from '$lib/components/admin/Navigation/Sidebar.svelte';
    import Toast from '$lib/components/admin/Feedback/Toast.svelte';
    import { onMount } from 'svelte';
    import { page } from '$app/stores';

    // Global toast state management (simple implementation)
    // In a real app, use a store
    let toasts: Array<{ id: number; message: string; type: 'success' | 'error' | 'info' }> = [];
    let nextToastId = 0;

    function addToast(event: CustomEvent) {
        const { message, type } = event.detail;
        const id = nextToastId++;
        toasts = [...toasts, { id, message, type }];
    }

    function removeToast(id: number) {
        toasts = toasts.filter(t => t.id !== id);
    }
    
    // Listen for toast events from children
    // This is a bit hacky, normally use a store, but for MVP it works if components dispatch to window or use context
    // Actually, components inside <slot> can't easily dispatch to layout without context or store.
    // Let's use a store-like pattern by attaching to window for MVP or just exporting a store.
    // For now, I'll rely on individual pages to handle their toasts or I'll add a store later.
    // But the plan mentions Toast component.
    
    // Let's create a simple store for toasts in a separate file if needed, 
    // but for now let's just render the layout. Pages will likely instantiate Toast locally 
    // or I'll add a store in `frontend/src/lib/stores/toast.ts` if I want global toasts.
    // The plan didn't specify a store, so I'll assume local usage or I'll add a basic store now.
</script>

<!-- 
    Note: To make toasts work globally, we'd ideally use a Svelte store.
    For this implementation, we'll let pages handle their own toasts locally
    or we can add a store later.
-->

<div class="admin-layout">
    <Sidebar />
    <main>
        <slot />
    </main>
</div>

<style>
    .admin-layout {
        display: flex;
        min-height: 100vh;
        background: #111;
    }

    main {
        flex: 1;
        margin-left: 250px; /* Sidebar width */
        padding: 2rem;
        width: calc(100% - 250px);
    }

    @media (max-width: 768px) {
        main {
            margin-left: 0;
            width: 100%;
        }
    }
</style>
