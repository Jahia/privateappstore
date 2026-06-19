import React, {useEffect, useState} from 'react';
import {useMutation, useQuery} from '@apollo/client';
import {useTranslation} from 'react-i18next';
import {Button, Field, Input} from '@jahia/moonstone';
import styles from './CategorySettings.scss';
import {
    ADD_FORGE_CATEGORY,
    DELETE_FORGE_CATEGORY,
    GET_CATEGORY_SETTINGS,
    SET_ROOT_CATEGORY,
    UPDATE_FORGE_CATEGORY_TITLES
} from './CategorySettings.gql';

export function CategorySettings({siteKey}) {
    const {t} = useTranslation('jahia-store');

    const [rootCategoryInput, setRootCategoryInput] = useState('');
    const [newCategoryName, setNewCategoryName] = useState('');
    const [editingUuid, setEditingUuid] = useState(null);
    const [editingTitles, setEditingTitles] = useState({});
    const [status, setStatus] = useState(null);
    const [statusMessage, setStatusMessage] = useState('');

    useEffect(() => {
        const prev = document.title;
        document.title = `${t('categories.title')} — ${siteKey} — Jahia`;
        return () => {
            document.title = prev;
        };
    }, [siteKey, t]);

    const {data, loading, error, refetch} = useQuery(GET_CATEGORY_SETTINGS, {
        variables: {siteKey},
        fetchPolicy: 'network-only'
    });

    const refetchQueries = [{query: GET_CATEGORY_SETTINGS, variables: {siteKey}}];

    const [setRootCategory] = useMutation(SET_ROOT_CATEGORY, {refetchQueries});
    const [addCategory] = useMutation(ADD_FORGE_CATEGORY, {refetchQueries});
    const [updateCategoryTitles] = useMutation(UPDATE_FORGE_CATEGORY_TITLES, {refetchQueries});
    const [deleteCategory] = useMutation(DELETE_FORGE_CATEGORY, {refetchQueries});

    if (loading) {
        return <div className={styles.category_loading}>{t('label.loading')}</div>;
    }

    if (error) {
        return (
            <div className={styles.category_error} role="alert">
                {t('errors.load.failed')}: {error.message}
            </div>
        );
    }

    const settings = data && data.forge && data.forge.categorySettings;
    if (!settings) {
        return (
            <div className={styles.category_error} role="alert">
                {t('errors.load.failed')}
            </div>
        );
    }

    const reportSuccess = key => {
        setStatus('success');
        setStatusMessage(t(key));
    };

    const reportError = err => {
        console.error(err);
        setStatus('error');
        setStatusMessage(err && err.message ? err.message : t('errors.update.failed'));
    };

    const handleSetRoot = async () => {
        const uuid = rootCategoryInput.trim();
        if (!uuid) {
            return;
        }

        try {
            await setRootCategory({variables: {siteKey, rootCategoryUuid: uuid}});
            setRootCategoryInput('');
            reportSuccess('categories.success.root');
        } catch (err) {
            reportError(err);
        }
    };

    const handleAdd = async () => {
        const name = newCategoryName.trim();
        if (!name) {
            return;
        }

        try {
            const {data: addData} = await addCategory({variables: {siteKey, name}});
            setNewCategoryName('');
            reportSuccess('categories.success.add');
            // Auto-open the editor for the new category so the admin can immediately
            // fill in the per-language titles.
            const newUuid = addData && addData.forge && addData.forge.addCategory;
            if (newUuid) {
                const refreshed = await refetch();
                const created = refreshed?.data?.forge?.categorySettings?.categories?.find(c => c.uuid === newUuid);
                if (created) {
                    openEditor(created);
                }
            }
        } catch (err) {
            reportError(err);
        }
    };

    const openEditor = category => {
        const titles = {};
        for (const lang of settings.siteLanguages) {
            const existing = category.titles.find(x => x.language === lang);
            titles[lang] = (existing && existing.title) || '';
        }

        setEditingUuid(category.uuid);
        setEditingTitles(titles);
    };

    const closeEditor = () => {
        setEditingUuid(null);
        setEditingTitles({});
    };

    const handleSaveTitles = async () => {
        const titlesPayload = Object.entries(editingTitles).map(([language, title]) => ({
            language,
            title: title || null
        }));
        try {
            await updateCategoryTitles({
                variables: {siteKey, uuid: editingUuid, titles: titlesPayload}
            });
            reportSuccess('categories.success.titles');
            closeEditor();
        } catch (err) {
            reportError(err);
        }
    };

    const handleDelete = async category => {
        // Refuse to silently delete in-use categories — the server permits it but
        // the admin probably wants to see the usages list first.
        if (category.usages && category.usages.length > 0) {
            const confirmed = window.confirm(t('categories.delete.confirmInUse', {
                count: category.usages.length
            }));
            if (!confirmed) {
                return;
            }
        }

        try {
            await deleteCategory({variables: {siteKey, uuid: category.uuid}});
            if (editingUuid === category.uuid) {
                closeEditor();
            }

            reportSuccess('categories.success.delete');
        } catch (err) {
            reportError(err);
        }
    };

    return (
        <div>
            <div className={styles.category_page_header}>
                <h2>{t('categories.title')} — {siteKey}</h2>
            </div>
            <div className={styles.category_container}>
                {status === 'success' && (
                    <div className={`${styles.category_alert} ${styles.success}`} role="status">{statusMessage}</div>
                )}
                {status === 'error' && (
                    <div className={`${styles.category_alert} ${styles.error}`} role="alert">{statusMessage}</div>
                )}

                <section className={styles.category_section}>
                    <h3 className={styles.category_section_title}>
                        {t('categories.root.heading')}
                    </h3>
                    <div className={styles.category_root_current}>
                        {settings.rootCategoryPath
                            ? `${settings.rootCategoryDisplayName} (${settings.rootCategoryPath})`
                            : t('categories.root.none')}
                    </div>
                    <div className={styles.category_root_row}>
                        <div className={styles.category_root_input}>
                            <Field label={t('categories.root.uuidField')} id="root-category-uuid">
                                <Input
                                    id="root-category-uuid"
                                    placeholder={t('categories.root.uuidPlaceholder')}
                                    value={rootCategoryInput}
                                    onChange={e => setRootCategoryInput(e.target.value)}
                                />
                            </Field>
                        </div>
                        <Button
                            type="button"
                            label={t('label.save')}
                            variant="primary"
                            isDisabled={!rootCategoryInput.trim()}
                            onClick={handleSetRoot}
                        />
                    </div>
                </section>

                {settings.rootCategoryUuid && (
                    <>
                        <section className={styles.category_section}>
                            <h3 className={styles.category_section_title}>
                                {t('categories.list.heading')}
                            </h3>
                            {settings.categories.length === 0 ? (
                                <div className={styles.category_item_meta}>{t('categories.list.empty')}</div>
                            ) : (
                                <ul className={styles.category_list}>
                                    {settings.categories.map(c => (
                                        <li key={c.uuid} className={styles.category_item}>
                                            <div>
                                                <div>{c.displayName}</div>
                                                <div className={styles.category_item_meta}>
                                                    {c.titles.filter(x => x.title).map(x => x.language).join(', ') || t('categories.list.untranslated')}
                                                    {c.usages.length > 0 && (
                                                        <> · {t('categories.list.usages', {count: c.usages.length})}</>
                                                    )}
                                                </div>
                                            </div>
                                            <div className={styles.category_item_actions}>
                                                <Button
                                                    type="button"
                                                    label={t('label.edit')}
                                                    variant="secondary"
                                                    size="small"
                                                    onClick={() => openEditor(c)}
                                                />
                                                <Button
                                                    type="button"
                                                    label={t('label.delete')}
                                                    variant="danger"
                                                    size="small"
                                                    onClick={() => handleDelete(c)}
                                                />
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>

                        {editingUuid && (
                            <section className={styles.category_editor}>
                                <h3 className={styles.category_section_title}>
                                    {t('categories.editor.heading')}
                                </h3>
                                <div className={styles.category_editor_titles}>
                                    {settings.siteLanguages.map(lang => (
                                        <Field key={lang} label={lang} id={`title-${lang}`}>
                                            <Input
                                                id={`title-${lang}`}
                                                value={editingTitles[lang] || ''}
                                                onChange={e => setEditingTitles(prev => ({...prev, [lang]: e.target.value}))}
                                            />
                                        </Field>
                                    ))}
                                </div>
                                <div className={styles.category_editor_actions}>
                                    <Button
                                        type="button"
                                        label={t('label.save')}
                                        variant="primary"
                                        onClick={handleSaveTitles}
                                    />
                                    <Button
                                        type="button"
                                        label={t('label.cancel')}
                                        variant="secondary"
                                        onClick={closeEditor}
                                    />
                                </div>
                            </section>
                        )}

                        <section className={styles.category_section}>
                            <h3 className={styles.category_section_title}>
                                {t('categories.add.heading')}
                            </h3>
                            <div className={styles.category_add_row}>
                                <div className={styles.category_root_input}>
                                    <Field label={t('categories.add.nameField')} id="new-category-name">
                                        <Input
                                            id="new-category-name"
                                            placeholder={t('categories.add.namePlaceholder')}
                                            value={newCategoryName}
                                            onChange={e => setNewCategoryName(e.target.value)}
                                        />
                                    </Field>
                                </div>
                                <Button
                                    type="button"
                                    label={t('label.add')}
                                    variant="primary"
                                    isDisabled={!newCategoryName.trim()}
                                    onClick={handleAdd}
                                />
                            </div>
                        </section>
                    </>
                )}
            </div>
        </div>
    );
}

export default CategorySettings;
