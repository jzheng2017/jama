package nl.jiankai.refactoringminer;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.Visibility;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.*;
import nl.jiankai.api.*;
import nl.jiankai.api.project.GitRepository;
import nl.jiankai.api.project.Position;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RefactoringMinerImpl implements RefactoringMiner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefactoringMinerImpl.class);
    public RefactoringMinerImpl() {
    }

    @Override
    public Collection<Refactoring> detectRefactoringBetweenCommit(GitRepository gitRepository, String startCommitId, String endCommitId, Set<RefactoringType> refactoringTypes) {
        List<Refactoring> detectedRefactorings = new ArrayList<>();
        GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMiner gitHistoryRefactoringMiner = new GitHistoryRefactoringMinerImpl();
        try (Repository repository = gitService.openRepository(gitRepository.getLocalPath().getAbsolutePath())) {
            gitHistoryRefactoringMiner.detectBetweenCommits(repository, startCommitId, endCommitId, new RefactoringHandler() {
                int sequence = 1;

                @Override
                public void handle(String commitId, List<org.refactoringminer.api.Refactoring> refactorings) {
                    List<Refactoring> foundRefactoring = refactorings
                            .stream()
                            .filter(r -> {
                                RefactoringType refactoringType = convertRefactoringType(r.getRefactoringType());
                                return refactoringType != RefactoringType.UNKNOWN && (refactoringTypes.isEmpty() || refactoringTypes.contains(refactoringType)) && isPublic(r) && !inTestDirectory(r);
                            })
                            .map(r -> new Refactoring(commitId, sequence, getBeforeElement(r), getAfterElement(r), convertRefactoringType(r.getRefactoringType()), getContext(r)))
                            .toList();
                    if (!foundRefactoring.isEmpty()) {
                        detectedRefactorings.addAll(foundRefactoring);
                        sequence++;
                    }
                }

                private Map<String, Object> getContext(org.refactoringminer.api.Refactoring r) {
                    if (r instanceof ReorderParameterRefactoring ropr) {
                        return Map.of(
                                "before", ropr.getParametersBefore().stream().map(v -> Map.of("type", v.getType().toString(), "name", v.getVariableName())).toList(),
                                "after", ropr.getParametersAfter().stream().map(v ->  Map.of("type", v.getType().toString(), "name", v.getVariableName())).toList()
                        );
                    } else if (r instanceof AddParameterRefactoring apr) {
                        return Map.of(
                                "before", apr.getOperationBefore().getParameterDeclarationList().stream().map(v -> Map.of("type", v.getType().toString(), "name", v.getVariableName())).toList(),
                                "after", apr.getOperationAfter().getParameterDeclarationList().stream().map(v -> Map.of("type", v.getType().toString(), "name", v.getVariableName())).toList()
                        );
                    } else if (r instanceof RemoveParameterRefactoring rpr) {
                        return Map.of(
                                "before", rpr.getOperationBefore().getParameterDeclarationList().stream().map(v -> Map.of("type", v.getType().toString(), "name", v.getVariableName())).toList(),
                                "after", rpr.getOperationAfter().getParameterDeclarationList().stream().map(v -> Map.of("type", v.getType().toString(), "name", v.getVariableName())).toList()
                        );
                    } else if (r instanceof RenameVariableRefactoring rvr) {
                        return Map.of(
                                "before", rvr.getOperationBefore().getParameterDeclarationList().stream().map(v -> Map.of("type", v.getType().toString(), "name", v.getVariableName())).toList(),
                                "after", rvr.getOperationAfter().getParameterDeclarationList().stream().map(v -> Map.of("type", v.getType().toString(), "name", v.getVariableName())).toList(),
                                "original", rvr.getOriginalVariable().getVariableName(),
                                "renamed", rvr.getRenamedVariable().getVariableName()
                        );
                    } else if (r instanceof ChangeVariableTypeRefactoring cvtr) {
                        return Map.of(
                                "before", cvtr.getOperationBefore().getParameterDeclarationList().stream().map(v -> Map.of("type", v.getType().toString(), "name", v.getVariableName())).toList(),
                                "after", cvtr.getOperationAfter().getParameterDeclarationList().stream().map(v -> Map.of("type", v.getType().toString(), "name", v.getVariableName())).toList(),
                                "changedTypeVariable", cvtr.getChangedTypeVariable().getVariableName()
                        );
                    } else if (r instanceof EncapsulateAttributeRefactoring ear) {
                        return Map.of(
                                "getter", getSignatureUMLOperation(ear.getAddedGetter()),
                                "setter", getSignatureUMLOperation(ear.getAddedSetter())
                        );
                    } else if (r instanceof AddThrownExceptionTypeRefactoring atetr) {
                        return Map.of("exception", atetr.getExceptionType().toQualifiedString());
                    }
                    return new HashMap<>();
                }
            });

            return detectedRefactorings;
        } catch (Exception e) {
            throw new IllegalArgumentException("Something went wrong with the repository '%s'".formatted(gitRepository.getId()), e);
        }
    }

    private boolean inTestDirectory(org.refactoringminer.api.Refactoring r) {
        String filePath = getBeforeFilePath(r);

        return filePath.startsWith("src/test") || filePath.startsWith("/src/test");
    }

    private boolean isPublic(org.refactoringminer.api.Refactoring r) {
        if (r instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationBefore().getVisibility() == Visibility.PUBLIC;
        } else if (r instanceof AddParameterRefactoring apr) {
            return apr.getOperationBefore().getVisibility() == Visibility.PUBLIC;
        } else if (r instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationBefore().getVisibility() == Visibility.PUBLIC;
        } else if (r instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOriginalVariable().getModifiers().stream().anyMatch(modifier -> modifier.getKeyword().equals("public"));
        } else if (r instanceof RenameOperationRefactoring ror) {
            return ror.getOriginalOperation().getVisibility() == Visibility.PUBLIC;
        } else if (r instanceof ReorderParameterRefactoring ropr) {
            return ropr.getOperationBefore().getVisibility() == Visibility.PUBLIC;
        } else if (r instanceof RenameVariableRefactoring rvr) {
            return rvr.getOriginalVariable().getModifiers().stream().anyMatch(modifier -> modifier.getKeyword().equals("public"));
        } else if (r instanceof AddThrownExceptionTypeRefactoring atet) {
            return atet.getOperationBefore().getVisibility() == Visibility.PUBLIC;
        } else if (r instanceof MoveOperationRefactoring mor) {
            return mor.getOriginalOperation().getVisibility() == Visibility.PUBLIC;
        } else if (r instanceof RenameClassRefactoring rcr) {
            return rcr.getOriginalClass().getVisibility() == Visibility.PUBLIC;
        } else if (r instanceof MoveClassRefactoring mcr) {
            return mcr.getOriginalClass().getVisibility() == Visibility.PUBLIC;
        } else if (r instanceof EncapsulateAttributeRefactoring ear) {
            return ear.getAttributeBefore().getVisibility() == Visibility.PUBLIC;
        }

        LOGGER.warn("Could not determine visibility of unsupported refactored code element with refactoring type {}", r.getRefactoringType());
        return false;
    }

    private Refactoring.CodeElement getBeforeElement(org.refactoringminer.api.Refactoring refactoring) {
        String filePath = getBeforeFilePath(refactoring);
        Position position = getBeforePosition(refactoring);
        return new Refactoring.CodeElement(getBeforeElementName(refactoring), getBeforePackagePath(refactoring), getBeforeSignature(refactoring), position, filePath);
    }

    private Refactoring.CodeElement getAfterElement(org.refactoringminer.api.Refactoring refactoring) {
        String filePath = getAfterFilePath(refactoring);
        Position position = getAfterPosition(refactoring);
        return new Refactoring.CodeElement(getAfterElementName(refactoring), getAfterPackagePath(refactoring), getAfterSignature(refactoring), position, filePath);
    }

    private String getBeforeSignature(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof MoveClassRefactoring || refactoring instanceof RenameClassRefactoring) {
            return getBeforeFullyQualifiedClassName(refactoring);
        } else if (refactoring instanceof EncapsulateAttributeRefactoring) {
            return getBeforeFullyQualifiedClassName(refactoring) + "#" + getBeforeElementName(refactoring);
        } else {
            String parameters = getBeforeParameters(refactoring);
            return getBeforeFullyQualifiedClassName(refactoring) + "#" + getBeforeElementName(refactoring) + (parameters.isEmpty() ? "" : parameters);
        }
    }

    private String getSignatureUMLOperation(UMLOperation umlOperation) {
        if (umlOperation == null) {
            return "";
        }
        return umlOperation.getClassName() + "#" + umlOperation.getName() + "(%s)".formatted(getParametersUMLOperation(umlOperation));
    }

    private String getParametersUMLOperation(UMLOperation umlOperation) {
        return umlOperation.getParameters().stream().filter(m -> "in".equals(m.getKind())).map(UMLParameter::getType).map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
    }

    private String getBeforeParameters(org.refactoringminer.api.Refactoring refactoring) {
        String parameters = "(";
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            parameters += getParametersUMLOperation(crtr.getOperationBefore());
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            parameters += getParametersUMLOperation(apr.getOperationBefore());
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            parameters += getParametersUMLOperation(rpr.getOperationBefore());
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            parameters += cvtr.getOperationBefore().getParameterTypeList().stream().map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            parameters += getParametersUMLOperation(ror.getOriginalOperation());
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            parameters += ropr.getParametersBefore().stream().map(VariableDeclaration::getType).map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            parameters += rvr.getOperationBefore().getParameterTypeList().stream().map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            parameters += atet.getOperationBefore().getParameterTypeList().stream().map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            parameters += mor.getOriginalOperation().getParameterTypeList().stream().map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        }

        return parameters + ")";
    }

    private String getAfterSignature(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof MoveClassRefactoring || refactoring instanceof RenameClassRefactoring) {
            return getAfterFullyQualifiedClassName(refactoring);
        } else if (refactoring instanceof EncapsulateAttributeRefactoring) {
            return getAfterFullyQualifiedClassName(refactoring) + "#" + getAfterElementName(refactoring);
        } else {
            String parameters = getAfterParameters(refactoring);
            return getAfterFullyQualifiedClassName(refactoring) + "#" + getAfterElementName(refactoring) + (parameters.isEmpty() ? "" : parameters);
        }
    }

    private String getAfterParameters(org.refactoringminer.api.Refactoring refactoring) {
        String parameters = "(";
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            parameters += crtr.getOperationAfter().getParameters().stream().filter(m -> "in".equals(m.getKind())).map(UMLParameter::getType).map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            parameters += apr.getOperationAfter().getParameters().stream().filter(m -> "in".equals(m.getKind())).map(UMLParameter::getType).map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            parameters += rpr.getOperationAfter().getParameters().stream().filter(m -> "in".equals(m.getKind())).map(UMLParameter::getType).map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            parameters += cvtr.getOperationAfter().getParameterTypeList().stream().map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            parameters += ror.getRenamedOperation().getParameters().stream().filter(m -> "in".equals(m.getKind())).map(UMLParameter::getType).map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            parameters += ropr.getParametersAfter().stream().map(VariableDeclaration::getType).map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            parameters += rvr.getOperationAfter().getParameterTypeList().stream().map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            parameters += atet.getOperationAfter().getParameterTypeList().stream().map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            parameters += mor.getMovedOperation().getParameterTypeList().stream().map(UMLType::toQualifiedString).collect(Collectors.joining(", "));
        }

        return parameters + ")";
    }

    private String getAfterFullyQualifiedClassName(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationAfter().getClassName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationAfter().getClassName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationAfter().getClassName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationAfter().getClassName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getRenamedOperation().getClassName();
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            return ropr.getOperationAfter().getClassName();
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            return rvr.getOperationAfter().getClassName();
        } else if (refactoring instanceof EncapsulateAttributeRefactoring ear) {
            return ear.getAttributeAfter().getClassName();
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            return atet.getOperationAfter().getClassName();
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            return mor.getMovedOperation().getClassName();
        } else if (refactoring instanceof MoveClassRefactoring mcr) {
            return mcr.getMovedClassName();
        } else if (refactoring instanceof RenameClassRefactoring rcr) {
            return rcr.getRenamedClassName();
        }

        return "";
    }

    private String getBeforeFullyQualifiedClassName(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationBefore().getClassName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationBefore().getClassName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationBefore().getClassName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationBefore().getClassName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getOriginalOperation().getClassName();
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            return ropr.getOperationBefore().getClassName();
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            return rvr.getOperationBefore().getClassName();
        } else if (refactoring instanceof EncapsulateAttributeRefactoring ear) {
            return ear.getAttributeBefore().getClassName();
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            return atet.getOperationBefore().getClassName();
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            return mor.getOriginalOperation().getClassName();
        } else if (refactoring instanceof MoveClassRefactoring mcr) {
            return mcr.getOriginalClassName();
        }else if (refactoring instanceof RenameClassRefactoring rcr) {
            return rcr.getOriginalClassName();
        }

        return "";
    }

    private String getBeforeFilePath(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getOriginalOperation().getLocationInfo().getFilePath();
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            return ropr.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            return rvr.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof EncapsulateAttributeRefactoring ear) {
            return ear.getAttributeBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            return atet.getOperationBefore().getLocationInfo().getFilePath();
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            return mor.getOriginalOperation().getLocationInfo().getFilePath();
        } else if (refactoring instanceof MoveClassRefactoring mcr) {
            return mcr.getOriginalClass().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RenameClassRefactoring rcr) {
            return rcr.getOriginalClass().getLocationInfo().getFilePath();
        }

        return "";
    }

    private Position getBeforePosition(org.refactoringminer.api.Refactoring refactoring) {
        CodeRange codeRange = null;
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            codeRange = crtr.getOperationBefore().codeRange();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            codeRange = apr.getOperationBefore().codeRange();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            codeRange = rpr.getOperationBefore().codeRange();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            codeRange = cvtr.getOperationBefore().codeRange();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            codeRange = ror.getSourceOperationCodeRangeBeforeRename();
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            codeRange = ropr.getOperationBefore().codeRange();
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            codeRange = rvr.getOperationBefore().codeRange();
        } else if (refactoring instanceof EncapsulateAttributeRefactoring ear) {
            codeRange = ear.getAttributeBefore().codeRange();
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            codeRange = atet.getOperationBefore().codeRange();
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            codeRange = mor.getOriginalOperation().codeRange();
        } else if (refactoring instanceof MoveClassRefactoring mcr) {
            codeRange = mcr.getOriginalClass().codeRange();
        } else if (refactoring instanceof RenameClassRefactoring rcr) {
            codeRange = rcr.getOriginalClass().codeRange();
        }

        if (codeRange == null) {
            return null;
        } else {
            return new Position(codeRange.getStartColumn(), codeRange.getEndColumn(), codeRange.getStartLine(), codeRange.getEndLine());
        }
    }

    private String getBeforeElementName(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationBefore().getName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationBefore().getName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationBefore().getName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationBefore().getName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getOriginalOperation().getName();
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            return ropr.getOperationBefore().getName();
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            return rvr.getOperationBefore().getName();
        } else if (refactoring instanceof EncapsulateAttributeRefactoring ear) {
            return ear.getAttributeBefore().getName();
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            return atet.getOperationBefore().getName();
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            return mor.getOriginalOperation().getName();
        } else if (refactoring instanceof MoveClassRefactoring mcr) {
            return mcr.getOriginalClass().getNonQualifiedName();
        } else if (refactoring instanceof RenameClassRefactoring rcr) {
            return rcr.getOriginalClass().getNonQualifiedName();
        }

        return "";
    }

    private String getBeforePackagePath(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationBefore().getClassName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationBefore().getClassName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationBefore().getClassName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationBefore().getClassName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getOriginalOperation().getClassName();
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            return ropr.getOperationBefore().getClassName();
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            return rvr.getOperationBefore().getClassName();
        } else if (refactoring instanceof EncapsulateAttributeRefactoring ear) {
            return ear.getAttributeBefore().getClassName();
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            return atet.getOperationBefore().getClassName();
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            return mor.getOriginalOperation().getClassName();
        } else if (refactoring instanceof MoveClassRefactoring mcr) {
            return mcr.getOriginalClass().getPackageName();
        } else if (refactoring instanceof RenameClassRefactoring rcr) {
            return rcr.getOriginalClass().getPackageName();
        }

        return "";
    }

    private String getAfterFilePath(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getRenamedOperation().getLocationInfo().getFilePath();
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            return ropr.getOperationAfter().getClassName();
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            return rvr.getOperationAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof EncapsulateAttributeRefactoring ear) {
            return ear.getAttributeAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            return atet.getOperationAfter().getLocationInfo().getFilePath();
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            return mor.getMovedOperation().getLocationInfo().getFilePath();
        } else if (refactoring instanceof MoveClassRefactoring mcr) {
            return mcr.getMovedClass().getLocationInfo().getFilePath();
        } else if (refactoring instanceof RenameClassRefactoring rcr) {
            return rcr.getRenamedClass().getLocationInfo().getFilePath();
        }

        return "";
    }

    private Position getAfterPosition(org.refactoringminer.api.Refactoring refactoring) {
        CodeRange codeRange = null;
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            codeRange = crtr.getOperationAfter().codeRange();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            codeRange = apr.getOperationAfter().codeRange();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            codeRange = rpr.getOperationAfter().codeRange();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            codeRange = cvtr.getOperationAfter().codeRange();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            codeRange = ror.getTargetOperationCodeRangeAfterRename();
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            codeRange = ropr.getOperationAfter().codeRange();
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            codeRange = rvr.getOperationAfter().codeRange();
        } else if (refactoring instanceof EncapsulateAttributeRefactoring ear) {
            codeRange = ear.getAttributeAfter().codeRange();
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            codeRange = atet.getOperationAfter().codeRange();
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            codeRange = mor.getMovedOperation().codeRange();
        } else if (refactoring instanceof MoveClassRefactoring mcr) {
            codeRange = mcr.getMovedClass().codeRange();
        } else if (refactoring instanceof RenameClassRefactoring rcr) {
            codeRange = rcr.getRenamedClass().codeRange();
        }


        if (codeRange == null) {
            return null;
        } else {
            return new Position(codeRange.getStartColumn(), codeRange.getEndColumn(), codeRange.getStartLine(), codeRange.getEndLine());
        }
    }

    private String getAfterElementName(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationAfter().getName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationAfter().getName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationAfter().getName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationAfter().getName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getRenamedOperation().getName();
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            return ropr.getOperationAfter().getName();
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            return rvr.getOperationAfter().getName();
        } else if (refactoring instanceof EncapsulateAttributeRefactoring ear) {
            return ear.getAttributeAfter().getName();
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            return atet.getOperationAfter().getName();
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            return mor.getMovedOperation().getName();
        } else if (refactoring instanceof MoveClassRefactoring mcr) {
            return mcr.getMovedClass().getNonQualifiedName();
        } else if (refactoring instanceof RenameClassRefactoring rcr) {
            return rcr.getRenamedClass().getNonQualifiedName();
        }


        return "";
    }

    private String getAfterPackagePath(org.refactoringminer.api.Refactoring refactoring) {
        if (refactoring instanceof ChangeReturnTypeRefactoring crtr) {
            return crtr.getOperationAfter().getClassName();
        } else if (refactoring instanceof AddParameterRefactoring apr) {
            return apr.getOperationAfter().getClassName();
        } else if (refactoring instanceof RemoveParameterRefactoring rpr) {
            return rpr.getOperationAfter().getClassName();
        } else if (refactoring instanceof ChangeVariableTypeRefactoring cvtr) {
            return cvtr.getOperationAfter().getClassName();
        } else if (refactoring instanceof RenameOperationRefactoring ror) {
            return ror.getRenamedOperation().getClassName();
        } else if (refactoring instanceof ReorderParameterRefactoring ropr) {
            return ropr.getOperationAfter().getClassName();
        } else if (refactoring instanceof RenameVariableRefactoring rvr) {
            return rvr.getOperationAfter().getClassName();
        } else if (refactoring instanceof EncapsulateAttributeRefactoring ear) {
            return ear.getAttributeAfter().getClassName();
        } else if (refactoring instanceof AddThrownExceptionTypeRefactoring atet) {
            return atet.getOperationAfter().getClassName();
        } else if (refactoring instanceof MoveOperationRefactoring mor) {
            return mor.getMovedOperation().getClassName();
        } else if (refactoring instanceof MoveClassRefactoring mcr) {
            return mcr.getMovedClass().getPackageName();
        } else if (refactoring instanceof RenameClassRefactoring rcr) {
            return rcr.getRenamedClass().getPackageName();
        }

        return "";
    }


    private RefactoringType convertRefactoringType(org.refactoringminer.api.RefactoringType type) {
        return switch (type) {
            case RENAME_METHOD -> RefactoringType.METHOD_NAME;
            case CHANGE_RETURN_TYPE -> RefactoringType.CHANGE_RETURN_TYPE;
            case CHANGE_PARAMETER_TYPE -> RefactoringType.CHANGE_PARAMETER_TYPE;
            case REMOVE_PARAMETER -> RefactoringType.REMOVE_PARAMETER;
            case ADD_PARAMETER -> RefactoringType.ADD_PARAMETER;
            case RENAME_PARAMETER -> RefactoringType.RENAME_PARAMETER;
            case ENCAPSULATE_ATTRIBUTE -> RefactoringType.ENCAPSULATE_ATTRIBUTE;
            case MOVE_CLASS -> RefactoringType.MOVE_CLASS;
            case RENAME_CLASS -> RefactoringType.CLASS_NAME;
            case RENAME_PACKAGE -> RefactoringType.PACKAGE_NAME;
            case REORDER_PARAMETER -> RefactoringType.REORDER_PARAMETER;
            case ADD_THROWN_EXCEPTION_TYPE -> RefactoringType.ADD_THROWN_EXCEPTION;
            case MOVE_OPERATION -> RefactoringType.MOVE_METHOD;
            default -> RefactoringType.UNKNOWN;
        };
    }
}
