package custom.senior.apuracao;

import java.util.List;

import org.joda.time.LocalDate;

import com.senior.ContextoGeralRH;
import com.senior.dataset.ICursor;
import com.senior.dataset.MappedParamProvider;
import com.senior.rh.Colaborador;
import com.senior.rh.entities.readonly.IR011PBH;
import com.senior.rh.entities.readonly.IR011PER;
import com.senior.rh.entities.readonly.IR038AFA;
import com.senior.rh.entities.readonly.IR044CAL;
import com.senior.rh.entities.readonly.IR066SIT;
import com.senior.rh.ponto.colaborador.Compensacao;
import com.senior.rh.ponto.colaborador.Horario;
import com.senior.rule.Rule;
//import com.senior.ContextoGeralRH;
//import com.senior.rh.entities.readonly.IR066SIT;

@Rule(description = "Regra Executado no Calculo da Apuração.")
public class RegraApuracao extends Apuracao {

	@Override
	public void execute() {
		// Obtém contexto da apuração
		ContextoApuracao apuracao = getContainer().getContextoApuracao();

		// Obtém informações do colaborador
		Colaborador colaborador = apuracao.getColaborador();

		// Obtém Contexto Geral
		ContextoGeralRH geral = getContainer().getContextoGeral();

		LocalDate newBhr = LocalDate.parse("2009-02-26");
		LocalDate datZer = LocalDate.parse("1900-12-31");
		// LocalDate iniApu = LocalDate.parse("1900-12-31");
		LocalDate datIni = LocalDate.parse("1900-12-31");

		// Dados referentes ao colaborador
		int numEmp = colaborador.getNumeroEmpresa();    // NumEmp
		int tipCol = colaborador.getTipoColaborador();  // TipCol
		int numCad = colaborador.getNumCad();           // Numcad

		// Obtém o objeto horário do colaborador processado na data
		Horario horario = apuracao.getHorario();

		// int escEmp = colaborador.getCodigoEscala(); // EscEmp
		int escEmp = apuracao.getEscala().getCodigo();
		int iCodSin = apuracao.getHistoricoSindicato().getCodSin();
		int sitAfa = 0;

		// Data do processamento
		LocalDate datPro = apuracao.getData();

		// Data final da da Apuração
		LocalDate datFim = apuracao.getDataFinal();

		// Código da Situação de "Extras Separação Após"
		int horExt = 0;
		int extVia = 0;

		if (apuracao.getHorSit(2) == 0) {

			// Bloco contendo toda a regra e validações relacionada a apuração
			// Definição do codigo do Banco de horas
			int codBho = 0;
			if (numEmp == 14 && (datFim.isAfter(newBhr) || datFim.isEqual(newBhr))) {
				codBho = 200;
			} else if (numEmp == 14 && datFim.isBefore(newBhr)) {
				codBho = 03;
			} else if (numEmp == 2 && (datFim.isAfter(newBhr) || datFim.isEqual(newBhr))) {
				codBho = 101;
			} else if (numEmp == 2 && datFim.isBefore(newBhr)) {
				codBho = 04;
			} else if (numEmp == 24 && (datFim.isAfter(newBhr) || datFim.isEqual(newBhr))) {
				codBho = 201;
			} else if (numEmp == 15 || numEmp == 16 || numEmp == 17 || numEmp == 34) {
				codBho = 0;
			}

			// Gerar adicional noturno @
			int adnNot = apuracao.getHorSit(27) + apuracao.getHorSit(39) + apuracao.getHorSit(41)
					+ apuracao.getHorSit(100) + apuracao.getHorSit(302) + apuracao.getHorSit(304)
					//+ apuracao.getHorSit(306) + apuracao.getHorSit(308) + apuracao.getHorSit(313)
					// Tirado dia 21/05 Empresa Mobilis - colaborador 17 dia 30/04 calculando adicional a mais
					+ apuracao.getHorSit(306) + apuracao.getHorSit(313) + + apuracao.getHorSit(308)
					+ apuracao.getHorSit(315) + apuracao.getHorSit(317) + apuracao.getHorSit(319)
					+ apuracao.getHorSit(329);
			apuracao.setHorSit(100, adnNot);

			
			// Converte Horas Extras Noturnas para HE Diurnas
			apuracao.setHorSit(301, apuracao.getHorSit(301) + apuracao.getHorSit(302));
			apuracao.setHorSit(302, 0);
			apuracao.setHorSit(303, apuracao.getHorSit(303) + apuracao.getHorSit(304));
			apuracao.setHorSit(304, 0);
			apuracao.setHorSit(305, apuracao.getHorSit(305) + apuracao.getHorSit(306));
			apuracao.setHorSit(306, 0);
			apuracao.setHorSit(307, apuracao.getHorSit(307) + apuracao.getHorSit(308));
			apuracao.setHorSit(308, 0);

			apuracao.setHorSit(312, apuracao.getHorSit(312) + apuracao.getHorSit(313));
			apuracao.setHorSit(313, 0);
			apuracao.setHorSit(314, apuracao.getHorSit(314) + apuracao.getHorSit(315));
			apuracao.setHorSit(315, 0);
			apuracao.setHorSit(316, apuracao.getHorSit(315) + apuracao.getHorSit(316));
			apuracao.setHorSit(317, 0);
			apuracao.setHorSit(318, apuracao.getHorSit(318) + apuracao.getHorSit(319));
			apuracao.setHorSit(319, 0);

			saiBho:
				
				
			/*--------------------------------------------------------------------
			 *        1. Tratamento para colaboradores com Banco de Horas         *
			 ---------------------------------------------------------------------*/
			if (codBho > 0) {

				int isit301 = apuracao.getHorSit(301);
				int isit309 = apuracao.getHorSit(309);

				if (isit301 > isit309)
					isit309 = isit301;

				int extSem = 0;
				int extDia = 0;

				// 1.1. Monta Parâmetros do cursor R011PER
				MappedParamProvider paramsR011 = new MappedParamProvider();
				paramsR011.setParam("codBhr", codBho);
				paramsR011.setParam("datPro", datPro);

				LocalDate datVer = LocalDate.parse("1900-12-31");
				int temPerBho = 0;

				// 1.2. Encontra a data inicial e qtde meses em BH criados pelo sistema
				try {
					ICursor<IR011PER> cCursor_R011 = getContainer().getEntitySession().newCursor(IR011PER.class);
					cCursor_R011.addFilter("CodBHr = :codBhr AND DatIni <= :datPro AND DatFim >= :datPro", paramsR011);
					try {
						cCursor_R011.open();
						if (cCursor_R011.next()) {
							IR011PER eR011PER = cCursor_R011.read();
							datVer = eR011PER.getDatIni();

							temPerBho = 1;
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						cCursor_R011.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				
				// 1.3. Se o BH foi criado pelo Gestão do Ponto, encontra a data inicial e qtde meses
				if (temPerBho == 0) {
					
					// 1.3.1. Parâmetros do cursor R011PBH
					MappedParamProvider paramsR011P = new MappedParamProvider();
					paramsR011P.setParam("codBhP", codBho);
					paramsR011P.setParam("datPrP", datPro);

					// 1.3.2. Encontra a data inicial e qtde meses em BH criados pelo sistema
					try {
						ICursor<IR011PBH> cCursor_R011P = getContainer().getEntitySession().newCursor(IR011PBH.class);
						cCursor_R011P.addFilter("CodBhr = :codBhP AND DatIni <= :datPrP AND DatFim >= :datPrP",
								paramsR011P);
						try {
							cCursor_R011P.open();
							if (cCursor_R011P.next()) {
								IR011PBH eR011PBH = cCursor_R011P.read();
								datVer = eR011PBH.getDatIni();
							}
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							cCursor_R011P.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				
				// 1.4. Monta a Data de verificação, conforme datas encontradas no cursor anterior, do Periodo de BH
				String dateVer = datVer.getYear() + "-" + datVer.getMonthOfYear() + "-1";
				LocalDate perRef = LocalDate.parse(dateVer);

				
				// 1.5. Parâmetros do cursor R044Cal - Código de Cálculo
				MappedParamProvider paramsR044 = new MappedParamProvider();
				paramsR044.setParam("numEmp", numEmp);
				paramsR044.setParam("perRef", perRef);

				
				// 1.6. Busca a Data de Inicio da Apuração
				try {
					ICursor<IR044CAL> cCursor_R044 = getContainer().getEntitySession().newCursor(IR044CAL.class);
					cCursor_R044.addFilter("NumEmp = :numEmp AND PerRef = :perRef AND TipCal = 11", paramsR044);

					try {
						cCursor_R044.open();
						if (cCursor_R044.next()) {
							IR044CAL eR044Cal = cCursor_R044.read();
							datVer = eR044Cal.getIniApu();
							// iniApu = eR044Cal.getIniApu();
							// datCmp = eR044Cal.getFimApu();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						cCursor_R044.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				
				// 1.7. Se a Data de Processamento for menor que inicio do BH
				if (datPro.isBefore(datVer)) {
					codBho = 0;
					break saiBho;
				}

				
				// 1.8. Parâmetros do cursor R038Afa - Afastamentos
				MappedParamProvider paramsR038 = new MappedParamProvider();
				paramsR038.setParam("numEmp", numEmp);
				paramsR038.setParam("tipCol", tipCol);
				paramsR038.setParam("numCad", numCad);
				paramsR038.setParam("datPro", datPro);
				paramsR038.setParam("datZer", datZer);

				
				// 1.9. Cursor R038Afa para verificaar se o colaborador está afastado na DatPro
				try {
					ICursor<IR038AFA> cCursor_R038 = getContainer().getEntitySession().newCursor(IR038AFA.class);
					cCursor_R038.addFilter(
							"NumEmp = :numEmp AND TipCol = :tipCol AND NumCad = :numCad AND DatAfa <= :datPro AND (DatTer >= :datPro OR DatTer = :datZer)",
							paramsR038);
					try {
						cCursor_R038.open();
						if (cCursor_R038.next()) {
							IR038AFA eR038Afa = cCursor_R038.read();
							sitAfa = eR038Afa.getSitAfa();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						cCursor_R038.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				
				// 1.10. Se não tiver afastamento, logo a situação do colaborador é '1 - Trabalhando'
				if (sitAfa <= 0) {
					sitAfa = 1;
				}

				
				// 1.11. Tratamento para Domingos e Folgas, onde não haverá BH para quem está na Situação '26 - Viagem a Serviço'
				if (apuracao.getHorSit(1) > 0 && sitAfa == 26) {
					apuracao.setHorSit(26, apuracao.getHorSit(1));
					apuracao.setHorSit(1, 0);
				}
				if (apuracao.getHorSit(51) > 0 && sitAfa == 26) {
					apuracao.setHorSit(27, apuracao.getHorSit(51));
					apuracao.setHorSit(51, 0);
				}

				
				// 1.12. Tratamento para domingos e folgas, onde não haverá BH para quem está na Situação '26 - Viagem a Serviço'
				if ((horario.getCodigo() == 9996) || (horario.getCodigo() == 9999)) {
					if (sitAfa == 26) {
						apuracao.setHorSit(316, apuracao.getHorSit(305));
						apuracao.setHorSit(318, apuracao.getHorSit(307));
						apuracao.setHorSit(305, 0);
						apuracao.setHorSit(307, 0);

						// Zera Horas Extras
						apuracao.setHorSit(16, 0);
						apuracao.setHorSit(66, 0);
						apuracao.setHorSit(300, 0);
						apuracao.setHorSit(301, 0);
						apuracao.setHorSit(303, 0);
						apuracao.setHorSit(312, 0);
						apuracao.setHorSit(314, 0);
						apuracao.setHorSit(320, 0);

						codBho = 0;
						break saiBho;
					}
				}

				
				// 1.13. Parâmetros do cursor R034Fun - Ficha Básica
				MappedParamProvider paramsR034 = new MappedParamProvider();
				paramsR034.setParam("numEmp", numEmp);
				paramsR034.setParam("tipCol", tipCol);
				paramsR034.setParam("numCad", numCad);

				char utiBho = 'N';

				
				// 1.14. Cursor R034Fun para verificar se o colaborador está assinalado para utilizar BH
				try {
					ICursor<UsuR034Fun> cCursor_R034 = getContainer().getEntitySession().newCursor(UsuR034Fun.class);
					cCursor_R034.addFilter("NumEmp = :numEmp AND TipCol = :tipCol AND NumCad = :numCad", paramsR034);
					try {
						cCursor_R034.open();
						if (cCursor_R034.next()) {
							UsuR034Fun eR034Fun = cCursor_R034.read();
							utiBho = eR034Fun.getUSU_UtiBho();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						cCursor_R034.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				
				// 1.15. Se o Colaborador não estiver assinalado para usar BH, sai do tratamento de BH na regra
				if (utiBho != 'S') {
					codBho = 0;
					break saiBho;
				}

				if (sitAfa != 14)
				  CalculaHoras(apuracao, geral);

				
				
				// TRATAMENTO FERIADO //
				/*---------------------------------------------------------------------------------------*
				* Quando houver feriado de 2ª. a 5ª. feira - lançar uma hora a débito no banco de horas  *
				* Quando houver feriado no sábado, lançar quatro horas a crédito no banco de horas.      *
				* Feriados na Sexta-feira não geram horas nem a crédito nem a débito                     *
				*---------------------------------------------------------------------------------------*/
				// 1.16. Se for Feriado, e o Colaborador estiver Trabalhando ou em Viagem a Serviço
				if ((horario.getCodigo() == 9997) && (sitAfa == 1 || sitAfa == 26)) {
					int diaSem = geral.getDiaSem(datPro);

					// 1.16.1. Se o DatPro for de Segunda a Quinta
					if ((diaSem >= 1) && (diaSem <= 4)) {
						// Viagem a Serviço
						if (sitAfa == 26) {
							apuracao.setHorSit(316, apuracao.getHorSit(305));
							apuracao.setHorSit(318, apuracao.getHorSit(307));
							apuracao.setHorSit(305, 0);
							apuracao.setHorSit(307, 0);
						} else if (sitAfa == 1) { // Trabalhando
							
							// Se escala DIFERENTE das Escalas que não compensam no Sábado
							if ((escEmp != 12) && (escEmp != 34) && (escEmp != 41) && (escEmp != 51) && (escEmp != 66)
									&& (escEmp != 67) && (escEmp != 210) && (escEmp != 233) && (escEmp != 236)) {
								apuracao.setHorSit(310, 60); // Debito BH  xxx
							}
						}
					} else if (diaSem == 5) { // Se DatPro for um Sexta
						
					} else if (diaSem == 6) { // Se DatPro for um Sábado
						// Se escala DIFERENTE das Escalas que não compensam no Sábado
						if ((escEmp != 12) && (escEmp != 34) && (escEmp != 41) && (escEmp != 51) && (escEmp != 66)
								&& (escEmp != 67) && (escEmp != 210) && (escEmp != 233) && (escEmp != 236)) {
							apuracao.setHorSit(309, 240); // Credito BH
						}
					}
				}

				int sitCmp = 0;
				int hor150 = 0;
				
				
				// 1.17. Se houver programação de compensação para um colaborador na
				// situação 320, estas horas extras deverão ir para B.H na proporção de 1,5 por 1
				// Clausula 4.10 do acordo de banco de horas
				java.util.List<Compensacao> compensacoes = apuracao.getCompensacoes(datPro);
				
				// percorre as compensações
				for (Compensacao compensacao : compensacoes) {
					sitCmp = compensacao.getCodSit();
				}

				// 1.18. Se tiver programação de Compensação
				if ((!compensacoes.isEmpty()) || (sitCmp == 320)) {
					if (apuracao.getHorSit(320) > 0) {
						hor150 = (int) (apuracao.getHorSit(320) * 1.5);
						apuracao.setHorSit(320, hor150);
					}
				}
				
				
				
				// 1.19. De Segunda à Quinta – As horas excedentes a jornada, até 1h
				// (para Escalas 12, 41 e 233 são 2h, e Escala 236 são 3h de limite)
				// para banco as demais Horas Extras à 50%
				if ((geral.getDiaSem(datPro) >= 1) && (geral.getDiaSem(datPro) <= 4)) {
					if ((escEmp == 12) || (escEmp == 41) || (escEmp == 233)) {
							
						// HE Viagem a Serviço, com mais de 2h de HE
						if ((sitAfa == 26) && (apuracao.getHorSit(312) > 120)) { 
							apuracao.setHorSit(312, apuracao.getHorSit(312) - 120);
							apuracao.setHorSit(309, 120);
						} else if (apuracao.getHorSit(301) > 120) { // Mais de 2h de HE
							apuracao.setHorSit(301, apuracao.getHorSit(301) - 120);
							apuracao.setHorSit(309, 120);
						} else if ((apuracao.getHorSit(301) > 0) || (apuracao.getHorSit(312) > 0)) {
							apuracao.setHorSit(309, apuracao.getHorSit(301) + apuracao.getHorSit(312));
							apuracao.setHorSit(301, 0);
							apuracao.setHorSit(312, 0);
						}
					} else if (escEmp == 236) {
						
						// HE Viagem a Serviço, com mais de 3h de HE
						if ((sitAfa == 26) && (apuracao.getHorSit(312) > 180)) { 
							apuracao.setHorSit(312, apuracao.getHorSit(312) - 180);
							apuracao.setHorSit(309, 180);
						} else if (apuracao.getHorSit(301) > 180) { // Mais de 3h de HE
							apuracao.setHorSit(301, apuracao.getHorSit(301) - 180);
							apuracao.setHorSit(309, 180);
						} else if ((apuracao.getHorSit(301) > 0) || (apuracao.getHorSit(312) > 0)) {
							apuracao.setHorSit(309, apuracao.getHorSit(301) + apuracao.getHorSit(312));
							apuracao.setHorSit(301, 0);
							apuracao.setHorSit(312, 0);
						}
					} else {
						
						// HE Viagem a Serviço, com mais de 1h de HE
						if ((sitAfa == 26) && (apuracao.getHorSit(312) > 60)) { 
							apuracao.setHorSit(312, apuracao.getHorSit(312) - 60);
							apuracao.setHorSit(309, 60);
						} else if (apuracao.getHorSit(301) > 60) { // Mais de 1h de HE
							apuracao.setHorSit(301, apuracao.getHorSit(301) - 60);
							apuracao.setHorSit(309, 60);
						} else if ((apuracao.getHorSit(301) > 0) || (apuracao.getHorSit(312) > 0)) {
							apuracao.setHorSit(309, apuracao.getHorSit(301) + apuracao.getHorSit(312));
							apuracao.setHorSit(301, 0);
							apuracao.setHorSit(312, 0);
						}
					}
				}

				
				
				// 1.20. Na Sexta-feira, até 2h para Banco e as demais HE 50%
				if ((geral.getDiaSem(datPro) == 5) && ((apuracao.getHorSit(301) > 0) || (apuracao.getHorSit(312) > 0))) {
					if (escEmp == 236) {
						if (sitAfa == 26) { // HE Viagem a Serviço, com mais de 3h de HE
							if (apuracao.getHorSit(312) > 180) {
								apuracao.setHorSit(312, apuracao.getHorSit(312) - 180);
								apuracao.setHorSit(309, 180);
							}
							else {
								apuracao.setHorSit(309, apuracao.getHorSit(312));
								apuracao.zeraHorasSituacao(312);
							}
						} 
						else { // Para demais escalas
							if (apuracao.getHorSit(301) > 180) { // Mais de 3h de HE
								apuracao.setHorSit(301, apuracao.getHorSit(301) - 180);
								apuracao.setHorSit(309, 180);
							}
							else {
								apuracao.setHorSit(309, apuracao.getHorSit(301));
								apuracao.zeraHorasSituacao(301);
							}
						}
					} else {
						if (apuracao.getHorSit(301) <= 120) {
							apuracao.setHorSit(309, apuracao.getHorSit(301));
							apuracao.zeraHorasSituacao(301);
						} else {
							apuracao.setHorSit(301, apuracao.getHorSit(301) - 120);
							apuracao.setHorSit(309, 120);
						}
					}

				}

				
				
				// Tratamento de Seg. a Sex. para Limite de 10h de HE semanais
				if ((geral.getDiaSem(datPro) >= 1) && (geral.getDiaSem(datPro) <= 5)) {
					int iDiaSem = geral.getDiaSem(datPro);
					if (iDiaSem == 1)
						datIni = datPro;
					else if (iDiaSem == 2)
						datIni = datPro.minusDays(1);
					else if (iDiaSem == 3)
						datIni = datPro.minusDays(2);
					else if (iDiaSem == 4)
						datIni = datPro.minusDays(3);
					else if (iDiaSem == 5)
						datIni = datPro.minusDays(4);

					// datIni = datPro.minusDays(5);

					// Parâmetros do cursor R066Sit
					MappedParamProvider paramsR066_3 = new MappedParamProvider();
					paramsR066_3.setParam("numEmp", numEmp);
					paramsR066_3.setParam("tipCol", tipCol);
					paramsR066_3.setParam("numCad", numCad);
					paramsR066_3.setParam("datIni", datIni);
					paramsR066_3.setParam("datPro", datPro);

					// CURSOR R066Sit para buscar as Horas Extras da Semana
					try {
						ICursor<IR066SIT> cCursor_R066 = getContainer().getEntitySession().newCursor(IR066SIT.class);
						cCursor_R066.addFilter("NumEmp = :numEmp AND " + "TipCol = :tipCol AND "
								+ "NumCad = :numCad AND " + "CodSit in (301,302,312,313) AND "
								+ "DatApu >= :datIni AND " + "DatApu < :datPro", paramsR066_3);
						try {
							cCursor_R066.open();
							while (cCursor_R066.next()) {
								IR066SIT eR066Sit = cCursor_R066.read();
								extSem = extSem + eR066Sit.getQtdHor();
							}
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							cCursor_R066.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (extSem > 600) { // Se na semana o colaborador já recebeu 10:00 de Horas Extras à 50%
						if (sitAfa == 26) {
							apuracao.setHorSit(314, apuracao.getHorSit(312));
							apuracao.setHorSit(312, 0);
						} else {
							apuracao.setHorSit(303, apuracao.getHorSit(301));
							apuracao.zeraHorasSituacao(301);
						}
					} else { // Se o colaborador não atingiu o limite de 10h de HE semanais
						int extDat = 0;
						if (sitAfa == 26) {
							extDat = apuracao.getHorSit(312);
							if ((extSem + extDat) <= 600) { // Se Não passar do limite de 10h de HE Semanais
								apuracao.setHorSit(312, extDat);
								apuracao.setHorSit(313, 0);
							} else { // Se passar do limite de 10h de HE Semanais
								apuracao.setHorSit(312, 600 - extSem);
								apuracao.setHorSit(314, extDat - apuracao.getHorSit(312));
							}
						} else {
							extDat = apuracao.getHorSit(301);
							if ((extSem + extDat) <= 600) { // Se Não passar do limite de 10h de HE Semanais
								apuracao.setHorSit(301, extDat);
								apuracao.setHorSit(302, 0);
							} else { // Se passar do limite de 10h de HE Semanais
								apuracao.setHorSit(301, 600 - extSem);
								apuracao.setHorSit(303, extDat - apuracao.getHorSit(301));
							}
						}
					}
				}

				// 1.21. Sábado – Limite de 10:00 para banco, as excedentes paga à
				// 50% (com limite de 10:00 extras à 50% na semana) as demais 90%
				if (geral.getDiaSem(datPro) == 6 && horario.getCodigo() != 9997) {
					datVer = datPro.minusDays(14);
					int priSab = 0;
					int segSab = 0;
					int limDia = 0;

					// if ((escEmp == 12) || (escEmp == 41)) {
					// limDia = 480;
					// } else {
					limDia = 600;
					// }

					// 1.21.1 Verifica se trabalho no primeiro sábado
					MappedParamProvider paramsR066 = new MappedParamProvider();
					paramsR066.setParam("numEmp", numEmp);
					paramsR066.setParam("tipCol", tipCol);
					paramsR066.setParam("numCad", numCad);
					paramsR066.setParam("datVer", datVer);

					// 1.21.2. Cursor na R066Sit para verificar se tem horas trabalhadas no primeiro sábado
					try {
						ICursor<IR066SIT> cCursor_R066 = getContainer().getEntitySession().newCursor(IR066SIT.class);
						cCursor_R066
								.addFilter("NumEmp = :numEmp AND " + "TipCol = :tipCol AND " + "NumCad = :numCad AND "
										+ "CodSit IN (1,51,301, 302, 303, 304, 305, 306, 307, 308, 309, 312, 313, 314, 315, 316, 317, 318, 319) AND "
										+ "DatApu = :datVer", paramsR066);

						try {
							cCursor_R066.open();
							while (cCursor_R066.next()) {
								IR066SIT eR066Sit = cCursor_R066.read();
								priSab = priSab + eR066Sit.getQtdHor();
							}
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							cCursor_R066.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					// 1.21.3. Se trabalhou no primeiro sábado, verifica se trabalhou no sabado da semana anterior ao DatPro
					if (priSab > 0) {
						datVer = datPro.minusDays(7);

						// 1.21.3.1 Verifica se trabalhou tres sabados seguidos
						MappedParamProvider paramsR066_2 = new MappedParamProvider();
						paramsR066_2.setParam("numEmp", numEmp);
						paramsR066_2.setParam("tipCol", tipCol);
						paramsR066_2.setParam("numCad", numCad);
						paramsR066_2.setParam("datVer", datVer);

						// 1.21.3.2. CURSOR R066Sit para buscar se trabalhou no sábado da semana anterior ao DatPro
						try {
							ICursor<IR066SIT> cCursor_R066 = getContainer().getEntitySession()
									.newCursor(IR066SIT.class);
							cCursor_R066.addFilter(
									"NumEmp = :numEmp AND " + "TipCol = :tipCol AND " + "NumCad = :numCad AND "
											+ "CodSit IN (1,51,301, 302, 303, 304, 305, 306, 307, 308, 309, 312, 313, 314, 315, 316, 317, 318, 319) AND "
											+ "DatApu = :datVer",
									paramsR066_2);

							try {
								cCursor_R066.open();
								while (cCursor_R066.next()) {
									IR066SIT eR066Sit = cCursor_R066.read();
									segSab = segSab + eR066Sit.getQtdHor();
								}
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								cCursor_R066.close();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					
					// 1.21.4. Se Trabalhou 3 sabados seguidos
					if (priSab > 0 && segSab > 0) {
						datIni = datPro.minusDays(5);

						// 1.21.4.1. Parâmetros do cursor R066Sit
						MappedParamProvider paramsR066_3 = new MappedParamProvider();
						paramsR066_3.setParam("numEmp", numEmp);
						paramsR066_3.setParam("tipCol", tipCol);
						paramsR066_3.setParam("numCad", numCad);
						paramsR066_3.setParam("datIni", datIni);
						paramsR066_3.setParam("datPro", datPro);

						// 1.21.4.2. CURSOR R066Sit para buscar as Horas Extras da Semana
						try {
							ICursor<IR066SIT> cCursor_R066 = getContainer().getEntitySession()
									.newCursor(IR066SIT.class);
							cCursor_R066.addFilter("NumEmp = :numEmp AND " + "TipCol = :tipCol AND "
									+ "NumCad = :numCad AND " + "CodSit in (301,302,312,313) AND "
									+ "DatApu >= :datIni AND " + "DatApu < :datPro", paramsR066_3);

							try {
								cCursor_R066.open();
								while (cCursor_R066.next()) {
									IR066SIT eR066Sit = cCursor_R066.read();
									extSem = extSem + eR066Sit.getQtdHor();
								}
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								cCursor_R066.close();
							}
						} catch (Exception e) {
							e.printStackTrace();

						}
						// 1.21.4.3. Se na semana o colaborador já recebeu 10:00 de Horas Extras à 50%
						if (extSem > 600) {
							apuracao.setHorSit(303, apuracao.getHorSit(301));
							apuracao.zeraHorasSituacao(301);
							apuracao.setHorSit(314, apuracao.getHorSit(312));
							apuracao.zeraHorasSituacao(312);
						} else { // Se o colaborador não atingiu o limite de 10h de HE semanais
							extDia = apuracao.getHorSit(301) + apuracao.getHorSit(312);
							if ((extSem + extDia) > 600) { // Se passar do limite de 10h de HE Semanais
								if (sitAfa == 26) {
									// Recebe o que falta pra chegar na 10h
									apuracao.setHorSit(312, 600 - extSem); 
									apuracao.setHorSit(314, extDia - apuracao.getHorSit(312));
								} else {
									apuracao.setHorSit(301, 600 - extSem);
									apuracao.setHorSit(303, extDia - apuracao.getHorSit(301));
								}
							}
						}
					} else { // 1.21.5. Se não Trabalhou 3 Sabados Seguidos
						extDia = apuracao.getHorSit(301) + apuracao.getHorSit(309) + apuracao.getHorSit(312);
						extSem = 0;
						
						// 1.21.5.1. Se as Extras do dia ultrapassar 10h ou 8h (Escalas 12 e 41)
						if (extDia > limDia) {
							datIni = datPro.minusDays(5);

							// Parâmetros do cursor R066Sit
							MappedParamProvider paramsR066_3 = new MappedParamProvider();
							paramsR066_3.setParam("numEmp", numEmp);
							paramsR066_3.setParam("tipCol", tipCol);
							paramsR066_3.setParam("numCad", numCad);
							paramsR066_3.setParam("datIni", datIni);
							paramsR066_3.setParam("datPro", datPro);

							// CURSOR R066Sit para buscar as Horas Extras da Semana
							try {
								ICursor<IR066SIT> cCursor_R066 = getContainer().getEntitySession()
										.newCursor(IR066SIT.class);
								cCursor_R066.addFilter("NumEmp = :numEmp AND " + "TipCol = :tipCol AND "
										+ "NumCad = :numCad AND " + "CodSit in (301,302,312,313) AND "
										+ "DatApu >= :datIni AND " + "DatApu < :datPro", paramsR066_3);

								try {
									cCursor_R066.open();
									while (cCursor_R066.next()) {
										IR066SIT eR066Sit = cCursor_R066.read();
										extSem = extSem + eR066Sit.getQtdHor();
									}
								} catch (Exception e) {
									e.printStackTrace();
								} finally {
									cCursor_R066.close();
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							if (extSem > 600) { // Se na semana o colaborador já recebeu 10:00 de Horas Extras à 50%
								apuracao.setHorSit(309, limDia);
								extDia = extDia - limDia;

								if (sitAfa == 26) {
									apuracao.setHorSit(314, extDia);
									apuracao.setHorSit(312, 0);
								} else {
									apuracao.setHorSit(303, extDia);
									apuracao.setHorSit(301, 0);
								}
							} else { // Se o colaborador não atingiu o limite de 10h de HE semanais
								apuracao.setHorSit(309, limDia);
								extDia = extDia - limDia;

								if (sitAfa == 26) {
									if ((extSem + extDia) <= 600) { // Se Não passar do limite de 10h de HE Semanais
										apuracao.setHorSit(312, extDia);
										apuracao.setHorSit(313, 0);
									} else { // Se passar do limite de 10h de HE Semanais
										apuracao.setHorSit(312, 600 - extSem);
										apuracao.setHorSit(314, extDia - apuracao.getHorSit(312));
									}
								} else {
									if ((extSem + extDia) <= 600) { // Se Não passar do limite de 10h de HE Semanais
										apuracao.setHorSit(301, extDia);
										apuracao.setHorSit(302, 0);
									} else { // Se passar do limite de 10h de HE Semanais
										apuracao.setHorSit(301, 600 - extSem);
										apuracao.setHorSit(303, extDia - apuracao.getHorSit(301));
									}
								}
							}
						} else {
							apuracao.setHorSit(309, extDia);
							apuracao.setHorSit(301, 0);
							apuracao.setHorSit(312, 0);
						}
					}
				}

				// 1.22. Tratamento para Faltas para quem tem BH
				int totFal = apuracao.getHorSit(15) + apuracao.getHorSit(20) + apuracao.getHorSit(21)
						+ apuracao.getHorSit(22) + apuracao.getHorSit(23) + apuracao.getHorSit(24)
						+ apuracao.getHorSit(65) + apuracao.getHorSit(310) + apuracao.getHorSit(410)
						+ apuracao.getHorSit(998);
				apuracao.setHorSit(310, totFal); // xxx

				// 1.23. Zera situações de Faltas
				apuracao.setHorSit(15, 0);
				apuracao.setHorSit(20, 0);
				apuracao.setHorSit(21, 0);
				apuracao.setHorSit(22, 0);
				apuracao.setHorSit(23, 0);
				apuracao.setHorSit(24, 0);
				apuracao.setHorSit(65, 0);
				apuracao.setHorSit(410, 0);
				apuracao.setHorSit(998, 0);
			}

			/*
			 * if ((iHorBat < iHorPreTra) && (apuracao.getHorSit(1) > 0) &&
			 * (iHorSit310 > 0)){ apuracao.setHorSit(1,
			 * apuracao.getHorSit(1,310)); apuracao.zeraHorasSituacao(310); if
			 * (apuracao.getHorSit(1) > iHorPreTra){ apuracao.setHorSit(310,
			 * apuracao.getHorSit(1) - iHorPreTra); apuracao.setHorSit(1,
			 * iHorPreTra); } } else if ((iHorBat > iHorPreTra) &&
			 * (apuracao.getHorSit(1) > 0) && (apuracao.getHorSit(309) > 0)){
			 * apuracao.setHorSit(1, apuracao.getHorSit(1,309));
			 * apuracao.zeraHorasSituacao(309); if (apuracao.getHorSit(1) >
			 * iHorPreTra){ apuracao.setHorSit(309, apuracao.getHorSit(1) -
			 * iHorPreTra); apuracao.setHorSit(1, iHorPreTra); } }
			 */
			/*
			 * else if ((apuracao.getHorSit(1) < iHorPreTra) &&
			 * (apuracao.getHorSit(309) > 0) && (apuracao.getHorSit(310) > 0)){
			 * 
			 * }
			 */

			// 1. Fim Tratamento Banco de Horas

			/*--------------------------------------------------------*
			*        2. Tratamento para quem NÃO tem BH               *
			*--------------------------------------------------------*/
			if (codBho == 0) {
				// 2.0. Definição da Data Inicial de acordo com o dia da Semana
				datIni = LocalDate.parse("1900-12-31");
			    int gDiaSem = geral.getDiaSem(datPro);
			    
				if (geral.getDiaSem(datPro) == 2) { // Terça-feira
					datIni = datPro.minusDays(1);
				} else if (geral.getDiaSem(datPro) == 3) { // Quarta-feira
					datIni = datPro.minusDays(2);
				} else if (geral.getDiaSem(datPro) == 4) { // Quinta-feira
					datIni = datPro.minusDays(3);
				} else if (geral.getDiaSem(datPro) == 5) { // Sexta-feira
					datIni = datPro.minusDays(4);
				} else if (geral.getDiaSem(datPro) == 6) { // Sábado
					datIni = datPro.minusDays(5);
				} else if (geral.getDiaSem(datPro) == 7) { // Domingo
					datIni = datPro.minusDays(6); 
				}

				// 2.1. Cursor para buscar as Horas Extras da Semana quando o DatPro for depois da Segunda-feira
				if (geral.getDiaSem(datPro) > 1) {
					// 2.1.1. Parâmetros do cursor R060Dsi - Definição de Situações
					MappedParamProvider paramsR066 = new MappedParamProvider();
					paramsR066.setParam("numEmp", numEmp);
					paramsR066.setParam("tipCol", tipCol);
					paramsR066.setParam("numCad", numCad);
					paramsR066.setParam("datIni", datIni);
					paramsR066.setParam("datPro", datPro);

					// 2.1.2. CURSOR R066Sit para buscar as Horas Extras da Semana
					try {
						ICursor<IR066SIT> cCursor_R066 = getContainer().getEntitySession().newCursor(IR066SIT.class);
						cCursor_R066.addFilter("NumEmp = :numEmp AND " + "TipCol = :tipCol AND "
								+ "NumCad = :numCad AND " + "CodSit in (301,302,312,313) AND "
								+ "DatApu >= :datIni AND " + "DatApu < :datPro", paramsR066);

						try {
							cCursor_R066.open();
							while (cCursor_R066.next()) {
								IR066SIT eR066Sit = cCursor_R066.read();
								int codSit = eR066Sit.getCodSit();
								if (codSit == 301) {
									horExt = horExt + eR066Sit.getQtdHor();
								} else {
									extVia = extVia + eR066Sit.getQtdHor();
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							cCursor_R066.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// 2.2. Parâmetros do cursor R006Esc
				MappedParamProvider paramsR006 = new MappedParamProvider();
				paramsR006.setParam("codEsc", escEmp);

				int horLim = 0;

				// 2.3. CURSOR R006Esc para Buscar o Limite de Horas das Semana na Escala
				try {
					ICursor<UsuR006Esc> cCursor_R006 = getContainer().getEntitySession().newCursor(UsuR006Esc.class);
					cCursor_R006.addFilter("CodEsc = :codEsc", paramsR006);

					try {
						cCursor_R006.open();
						if (cCursor_R006.next()) {
							UsuR006Esc eR006Esc = cCursor_R006.read();
							horLim = eR006Esc.getUSU_limsem();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						cCursor_R006.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}


				
				// =============================================================================================
				// ======= 2.3. Tratamento para horas extras normais (desconsiderando extras em viagens) =======
				// =============================================================================================
				// 2.4. Total de Horas Extras, incluindo o Dia do Processamento
				int totExt = horExt + apuracao.getHorSit(301);

				if (horExt >= horLim) {
					apuracao.setHorSit(303, apuracao.getHorSit(301));
					apuracao.setHorSit(301, 0);
				} else if (totExt > horLim) {
					apuracao.setHorSit(303, apuracao.getHorSit(301) - (horLim - horExt));
					apuracao.setHorSit(301, horLim - horExt);
				}

				
				// =============================================================================================
				// ========================= 2.6. Tratamento para horas extras em viagens ======================
				// =============================================================================================
				// 2.7. Total de Horas Extras, incluindo o Dia do Processamento
				horExt = extVia;
				totExt = horExt + apuracao.getHorSit(312);

				// 2.8. Total de Extras maior que o Limite e as Horas Extras da
				// Semana Menor que o Limite
				if (horExt >= horLim) {
					apuracao.setHorSit(314, apuracao.getHorSit(312));
					apuracao.setHorSit(312, 0);
				} else if (totExt > horLim) {
					apuracao.setHorSit(314, apuracao.getHorSit(312) - (horLim - horExt));
					apuracao.setHorSit(312, horLim - horExt);
				}
			}
			
			// 2. Fim Tratamento para que NÃO tem Banco de Horas
			if (iCodSin == 10) {
				CalculaHorasAdvogado(apuracao, geral);
			}
		}
		
		
		
		
		// Situação adicional - CH 233554 
		// Evanderson Pereira - 29/01/2025
		// Caso ocorra uma compensação '40 - Dispensa da empresa'
		int sitCmp = 0;

		// Verifica se existe compensação
		java.util.List<Compensacao> compensacoes = apuracao.getCompensacoes(datPro);
		
		// percorre as compensações
		for (Compensacao compensacao : compensacoes) {
			sitCmp = compensacao.getCodSit();
		}
		
		// sitCmp = 40 - Dispensa da empresa
		if (sitCmp == 40) {
		    if (apuracao.getHorSit(40) > 0) {
		        
		        // Verifica se existem marcações para o colaborador
		        FuncGlobais FuncGlobais = new FuncGlobais();
		        List<Marcacoes> listaMarcacoes = FuncGlobais.RetornaListaMarcacoes(apuracao, geral);

		        if (!listaMarcacoes.isEmpty()) {
		            // Se existirem marcações, aplica a lógica de compensação
		            int nTotCmp = apuracao.getHorSit(40); 
		            int nTotBhNeg = apuracao.getHorSit(310); 
		            int nTotBan = nTotCmp - nTotBhNeg;
		            int nTotTra = apuracao.getHorSit(1);
		        
		            // Verifica se existem horas negativas para o dia
		            if(nTotBhNeg == 0){
		            	apuracao.zeraHorasSituacao(309);
		            	
		            } 
		        }
		    }
		}
	}
	
	
	public void CalculaHorasAdvogado(ContextoApuracao apuracao, ContextoGeralRH geral) {
		int iHorPreTra = 0;
		int iHorBat = 0;

		if ((apuracao.getHorario().getCodigo() != 9996) && (apuracao.getHorario().getCodigo() != 9999)) {

			apuracao.zeraHorasSituacao(20, 22, 24, 305);

			FuncGlobais FuncGlobais = new FuncGlobais();
			List<Marcacoes> listaMarcacoes = FuncGlobais.RetornaListaMarcacoes(apuracao, geral);

			Colaborador colaborador = apuracao.getColaborador();
			for (int i = geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
					colaborador.getNumCad(), apuracao.getData()).getMarcacoes().size() - 1; i > 0; i--) {
				if (geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
								colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i).getMinutos() >= 
							geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
								colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i - 1).getMinutos())
				{
					iHorPreTra = iHorPreTra
						+ geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
								colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i).getMinutos()
						- geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
								colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i - 1).getMinutos();
				}else{
					iHorPreTra = iHorPreTra
							+ (geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
									colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i).getMinutos()  + 1440)
							- geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
									colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i - 1).getMinutos();
				}
				i--;// Tira 2
			}

			// Cálcula hora prevista de trabalho
			// Trecho removido 20/02/2013 
			// João Paulo 
			/*
			 * for (int i = listaMarcacoes.size() - 1; i > 0; i--) { iHorPreTra
			 * = iHorPreTra + listaMarcacoes.get(i).getiHorPre() -
			 * listaMarcacoes.get(i - 1).getiHorPre(); i--;// Tira 2 }
			 */

			// Por algum motivo as vezes está vindo negativo
			if (iHorPreTra < 0) {
				iHorPreTra = iHorPreTra * -1;
			}

			// Cálcula hora trabalhada
			for (int i = listaMarcacoes.size() - 1; i > 0; i--) {
				if (listaMarcacoes.get(i).getiHorBat() >= listaMarcacoes.get(i - 1).getiHorBat())
					iHorBat = iHorBat + listaMarcacoes.get(i).getiHorBat() - listaMarcacoes.get(i - 1).getiHorBat();
				else
					iHorBat = iHorBat + (listaMarcacoes.get(i).getiHorBat() + 1440)
							- listaMarcacoes.get(i - 1).getiHorBat();
				i--;// Tira 2
			}
			
			if (iHorBat > 0)
				apuracao.zeraHorasSituacao(15);

			if (iHorPreTra == iHorBat) {
				apuracao.setHorSit(1, iHorPreTra);

			} else if (iHorPreTra > iHorBat) {// Trabalhou menos do que previsto
				// verifica se chegou atrasado
				if (listaMarcacoes.get(0).getiHorBat() > listaMarcacoes.get(0).getiHorPre()) {
					// Se a Saida for Antes ou no horário previsto
					if ((listaMarcacoes.get(listaMarcacoes.size() - 1).getiHorBat() <= listaMarcacoes.get(listaMarcacoes.size() - 1).getiHorPre())
					 && (listaMarcacoes.get(listaMarcacoes.size() - 1).getdDatMar() == apuracao.getData())) {
						int iDif = listaMarcacoes.get(0).getiHorBat() - listaMarcacoes.get(0).getiHorPre();
						apuracao.setHorSit(20, iDif);
					}
					else { //Se for após o horário previsto
						// Atrasos
						int iDif = listaMarcacoes.get(0).getiHorBat() - listaMarcacoes.get(0).getiHorPre(); 
						// Quantidade de Horas após a hora prevista de saída
						int iExt = listaMarcacoes.get(listaMarcacoes.size() - 1).getiHorBat() - listaMarcacoes.get(listaMarcacoes.size() - 1).getiHorPre(); 
						
						// Se a quantidade de atraso for maior que o total trabalhado após o horário, diminuir dos atrasos o que foi trabalhar a mais
						if (iDif > iExt) {
							apuracao.setHorSit(20, iDif - iExt);
						}
						else {
							apuracao.zeraHorasSituacao(20);
						}						
					}
				}
				// verifica se saiu antes
				if ((listaMarcacoes.get(listaMarcacoes.size() - 1).getiHorBat() < listaMarcacoes
						.get(listaMarcacoes.size() - 1).getiHorPre())
						&& (listaMarcacoes.get(listaMarcacoes.size() - 1).getdDatMar() == apuracao.getData())) {					
					int iDif = listaMarcacoes.get(listaMarcacoes.size() - 1).getiHorPre()
							- listaMarcacoes.get(listaMarcacoes.size() - 1).getiHorBat();
					apuracao.setHorSit(22, iDif);
				}
				if ((iHorBat + apuracao.getHorSit(20, 22)) < iHorPreTra) {
					int iDif = iHorPreTra - (iHorBat + apuracao.getHorSit(20, 22));
					apuracao.setHorSit(24, iDif);
				}
				apuracao.setHorSit(1, iHorPreTra - apuracao.getHorSit(20, 22, 24));
			}			
            else { 
            	apuracao.setHorSit(1, iHorPreTra); 
            	apuracao.setHorSit(305, iHorBat - iHorPreTra); 
			}
			
			 
		}
	}

	public void CalculaHoras(ContextoApuracao apuracao, ContextoGeralRH geral) {
		int iHorPreTra = 0;
		int iHorBat = 0;
		int iHorBatAfa = 0;
				
		if ((apuracao.getHorario().getCodigo() != 9996) && (apuracao.getHorario().getCodigo() != 9997) && 
		    (apuracao.getHorario().getCodigo() != 9999) && ((apuracao.getMarcacoesRealizadas(true).size() % 2) == 0) && 
			(apuracao.getMarcacoesRealizadas(true).size() > 0)) {	
		
			FuncGlobais FuncGlobais = new FuncGlobais();
			List<Marcacoes> listaMarcacoes = FuncGlobais.RetornaListaMarcacoes(apuracao, geral);

			Colaborador colaborador = apuracao.getColaborador();
			for (int i = geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
					colaborador.getNumCad(), apuracao.getData()).getMarcacoes().size() - 1; i > 0; i--) {
				if (geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
								colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i).getMinutos() >= 
							geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
								colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i - 1).getMinutos())
				{
					iHorPreTra = iHorPreTra
						+ geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
								colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i).getMinutos()
						- geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
								colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i - 1).getMinutos();
				}else{
					iHorPreTra = iHorPreTra
							+ (geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
									colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i).getMinutos()  + 1440)
							- geral.getHorarioPrevistoColaborador(colaborador.getNumEmp(), colaborador.getTipCol(),
									colaborador.getNumCad(), apuracao.getData()).getMarcacoes().get(i - 1).getMinutos();
				}
				i--;// Tira 2
			}

			// Cálcula hora prevista de trabalho - Marcus Henrique 19/11/2011
			/*
			 * for (int i = listaMarcacoes.size() - 1; i > 0; i--) { iHorPreTra
			 * = iHorPreTra + listaMarcacoes.get(i).getiHorPre() -
			 * listaMarcacoes.get(i - 1).getiHorPre(); i--;// Tira 2 }
			 */

			// Por algum motivo as vezes está vindo negativo
			if (iHorPreTra < 0) {
				iHorPreTra = iHorPreTra * -1;
			}

			for (int i = listaMarcacoes.size() - 1; i > 0; i--) {
				if (listaMarcacoes.get(i).getiHorBat() >= listaMarcacoes.get(i - 1).getiHorBat())
					iHorBat = iHorBat + listaMarcacoes.get(i).getiHorBat() - listaMarcacoes.get(i - 1).getiHorBat();
				else{					
					iHorBat = iHorBat + (listaMarcacoes.get(i).getiHorBat() + 1440)
							- listaMarcacoes.get(i - 1).getiHorBat();
				}
				i--;// Tira 2
			}		
				
			/* Marcus Henrique 19/11/2011
			 * if (apuracao.getColaborador().getNumCad() == 1291){ if (iHorBat >
			 * 0) apuracao.setHorSit(2, iHorBat); else apuracao.setHorSit(3,
			 * iHorBat * -1); if (iHorPreTra > 0) apuracao.setHorSit(4,
			 * iHorPreTra); else apuracao.setHorSit(5, iHorPreTra * -1);
			 * 
			 * }else{
			 */
			iHorBatAfa = apuracao.getHorSit(413) + apuracao.getHorSit(414); // Situações de Saída para Médico
			
			if ((iHorPreTra > 0) || (iHorBat > 0)) {
				apuracao.zeraHorasSituacao(15, 20, 21, 22, 23, 24, 65, 305, 307, 310, 410, 998);

				if (iHorPreTra > (iHorBat + iHorBatAfa)) {
					apuracao.setHorSit(1, iHorBat);
					apuracao.setHorSit(310, iHorPreTra - iHorBat - iHorBatAfa); // xxx
					apuracao.zeraHorasSituacao(301, 309);
				} else if ((iHorBat + iHorBatAfa) > iHorPreTra) {
					apuracao.setHorSit(1, iHorPreTra - iHorBatAfa);
					apuracao.setHorSit(301, iHorBat - apuracao.getHorSit(1));

					/*
					 * apuracao.setHorSit(309, iHorBat - apuracao.getHorSit(1));
					 * apuracao.zeraHorasSituacao(310); if
					 * (apuracao.getHorSit(309) > 120) { apuracao.setHorSit(301,
					 * apuracao.getHorSit(309) - 120); apuracao.setHorSit(309,
					 * 120); } else { apuracao.zeraHorasSituacao(301); }
					 */
				} else if (iHorPreTra == (iHorBat + iHorBatAfa)) {
					apuracao.setHorSit(1, iHorBat);
					apuracao.zeraHorasSituacao(301, 309, 310);

				}
			}
		} else if ((apuracao.getHorSit(301) > 0) && ((geral.getDiaSem(apuracao.getData()) == 7) || (apuracao.getHorario().getCodigo() == 9997))) {
			if (apuracao.getHorSit(307) > 0) {
				apuracao.setHorSit(307, apuracao.getHorSit(301, 307));
				apuracao.zeraHorasSituacao(301);
			} else {
				apuracao.setHorSit(305, apuracao.getHorSit(301, 305));
				apuracao.zeraHorasSituacao(301);
			}
		}
	}
}
